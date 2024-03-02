package com.didan.azure.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;


import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClientBuilder;
import com.didan.azure.entity.FileInfo;
import com.didan.azure.entity.Sas;
import com.didan.azure.entity.Users;
import com.didan.azure.entity.keys.SasId;
import com.didan.azure.exception.AzureBlobStorageException;
import com.didan.azure.repository.FileInfoRepository;
import com.didan.azure.repository.SasRepository;
import com.didan.azure.repository.UserRepository;
import com.didan.azure.service.impl.AzureBlobServiceImpl;
import com.didan.azure.utils.JwtUtils;
import com.didan.azure.utils.SasUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;

@Service
public class AzureBlobService implements AzureBlobServiceImpl {
	@Value("${azure.storage.endpoint}")
	private String endpoint;
	private static Logger logger = LoggerFactory.getLogger(AzureBlobService.class);
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final BlobServiceClient blobServiceClient;
	private final JwtUtils jwtUtils;
	private final UserRepository userRepository;
	private final FileInfoRepository fileInfoRepository;
	private final SasRepository sasRepository;
	private final SasUtils sasUtils;

	@Autowired
	public AzureBlobService(HttpServletRequest request,
							HttpServletResponse response,
							BlobServiceClient blobServiceClient,
							JwtUtils jwtUtils,
							UserRepository userRepository,
							FileInfoRepository fileInfoRepository,
							SasRepository sasRepository,
							SasUtils sasUtils) {
		this.request = request;
		this.response = response;
		this.blobServiceClient = blobServiceClient;
		this.jwtUtils = jwtUtils;
		this.userRepository = userRepository;
		this.fileInfoRepository = fileInfoRepository;
		this.sasRepository = sasRepository;
		this.sasUtils = sasUtils;
	}

	// Upload many files to Azure Blob Storage
	@Override
	public List<String> upload(MultipartFile[] multipartFiles) throws IOException {
		String accessToken = jwtUtils.getTokenFromHeader(request);
		if (!StringUtils.hasText(accessToken)) {
			logger.info("Not Authorized");
		}
		String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
		Users user = userRepository.findFirstByUserId(userId);
		List<String> fileNames = new ArrayList<>();
		for (MultipartFile multipartFile : multipartFiles) {
			String fileName = multipartFile.getOriginalFilename();
			String[] split = fileName.split("\\.");
			String newFileName = split[0] + "_" + System.currentTimeMillis() + "." + split[1];
			BlobClient blobClient = new BlobClientBuilder()
					.endpoint(endpoint)
					.containerName(user.getUsername())
					.blobName(newFileName)
					.sasToken(user.getSasDirectory())
					.buildClient();
			blobClient.upload(multipartFile.getInputStream(), multipartFile.getSize(), true);
			BlobClient blob = blobServiceClient.getBlobContainerClient(user.getUsername()).getBlobClient(newFileName);
			String sasBlobToken = sasUtils.createServiceSASBlob(blob);
			FileInfo fileInfo = new FileInfo();
			fileInfo.setSasToken(sasBlobToken);
			fileInfo.setFileId(UUID.randomUUID().toString());
			fileInfo.setFileName(newFileName);
			fileInfo.setFilePath(blobClient.getBlobUrl());
			fileInfo.setUsers(user);
			fileInfoRepository.save(fileInfo);
			Sas sas = new Sas();
			sas.setSasId(new SasId(sasBlobToken, userId));
			sasRepository.save(sas);
			fileNames.add(newFileName);
		}
		return fileNames;
	}

	// Share file to another user by creating a new SAS token
	@Override
	public boolean shareFile(String fileName, String username) throws AzureBlobStorageException {
		try {
			String accessToken = jwtUtils.getTokenFromHeader(request);
			if (!StringUtils.hasText(accessToken)) {
				logger.info("Not Authorized");
			}
			String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
			Users user = userRepository.findFirstByUserId(userId);
			FileInfo file = user.getFileInfos().stream().filter(fileInfo -> (fileInfo.getFileName().equals(fileName) && fileInfo.getUsers().getUserId().equals(userId))).findFirst().orElse(null);
			if (file == null) {
				throw new AzureBlobStorageException("File not found");
			}
			Users shareUser = userRepository.findFirstByUsername(username);
			if (shareUser == null) {
				throw new AzureBlobStorageException("User not found");
			}
			if (shareUser.getUserId() == userId) {
				throw new AzureBlobStorageException("You can't share file to yourself");
			}
			Sas sasKey = file.getSass().stream().filter(sas -> sas.getUsers().getUserId().equals(shareUser.getUserId())).findFirst().orElse(null);
			if (sasKey != null) {
				throw new AzureBlobStorageException("File has been shared to this user");
			}
			Sas sas = new Sas();
			sas.setSasId(new SasId(file.getSasToken(), shareUser.getUserId()));
			sasRepository.save(sas);
		} catch (Exception e) {
			throw new AzureBlobStorageException(e.getMessage());
		}
		return true;
	}

	// Delete file from Azure Blob Storage
	@Override
	public boolean deleteFile(String fileName) throws AzureBlobStorageException {
		try {
			String accessToken = jwtUtils.getTokenFromHeader(request);
			if (!StringUtils.hasText(accessToken)) {
				logger.info("Not Authorized");
			}
			String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
			Users user = userRepository.findFirstByUserId(userId);
			FileInfo file = fileInfoRepository.findFirstByFileNameAndUsers_UserId(fileName, userId);
			if (file == null) {
				throw new AzureBlobStorageException("File not found or not authorized to delete");
			}
			List<Sas> sas = sasRepository.findBySasId_SasToken(file.getSasToken());
			sasRepository.deleteAll(sas);
			fileInfoRepository.delete(file);
			BlobClient blob = blobServiceClient.getBlobContainerClient(user.getUsername()).getBlobClient(fileName);
			blob.delete();
		} catch (Exception e) {
			throw new AzureBlobStorageException(e.getMessage());
		}
		return true;
	}

	// Delete many files from Azure Blob Storage
	@Override
	public boolean deleteManyFiles(String[] fileNames) throws AzureBlobStorageException {
		try {
			String accessToken = jwtUtils.getTokenFromHeader(request);
			if (!StringUtils.hasText(accessToken)) {
				logger.info("Not Authorized");
			}
			String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
			Users user = userRepository.findFirstByUserId(userId);
			for (String fileName : fileNames) {
				FileInfo file = fileInfoRepository.findFirstByFileNameAndUsers_UserId(fileName, userId);
				if (file == null) {
					throw new AzureBlobStorageException("File not found or not authorized to delete");
				}
				List<Sas> sas = sasRepository.findBySasId_SasToken(file.getSasToken());
				sasRepository.deleteAll(sas);
				fileInfoRepository.delete(file);
				BlobClient blob = blobServiceClient.getBlobContainerClient(user.getUsername()).getBlobClient(fileName);
				blob.delete();
			}
		} catch (Exception e) {
			throw new AzureBlobStorageException(e.getMessage());
		}
		return true;
	}

	//	// Delete all files from Azure Blob Storage
	@Override
	public boolean deleteAllFiles() throws AzureBlobStorageException {
		try {
			String accessToken = jwtUtils.getTokenFromHeader(request);
			if (!StringUtils.hasText(accessToken)) {
				logger.info("Not Authorized");
			}
			String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
			Users user = userRepository.findFirstByUserId(userId);
			List<FileInfo> files = fileInfoRepository.findByUsers_UserId(userId);
			if (files.isEmpty()) {
				throw new AzureBlobStorageException("Your storage is empty");
			}
			for (FileInfo file : files) {
				List<Sas> sas = sasRepository.findBySasId_SasToken(file.getSasToken());
				sasRepository.deleteAll(sas);
				fileInfoRepository.delete(file);
				BlobClient blob = blobServiceClient.getBlobContainerClient(user.getUsername()).getBlobClient(file.getFileName());
				blob.delete();
			}
		} catch (Exception e) {
			throw new AzureBlobStorageException(e.getMessage());
		}
		return true;
	}

	// Download file from url + sasToken and set Header Content-Disposition to "attachment; filename=\"" + fileName + "\""
	public byte[] downloadFile(String fileName) throws IOException, AzureBlobStorageException {
		String accessToken = jwtUtils.getTokenFromHeader(request);
		if (!StringUtils.hasText(accessToken)) {
			logger.info("Not Authorized");
		}
		String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
		Users user = userRepository.findFirstByUserId(userId);
		FileInfo file = fileInfoRepository.findByFileName(fileName);
		if (file == null) {
			throw new AzureBlobStorageException("File not found");
		}
		String sasToken = null;
		if (file.getUsers().getUserId().equals(userId)) {
			sasToken = file.getSasToken();
		} else {
			Sas sas = file.getSass().stream().filter(sas1 -> sas1.getUsers().getUserId().equals(userId)).findFirst().orElse(null);
			if (sas == null) {
				throw new AzureBlobStorageException("Not authorized to download this file");
			}
			sasToken = sas.getSasId().getSasToken();
		}
		BlobClient blob = new BlobClientBuilder()
				.endpoint(endpoint)
				.containerName(user.getUsername())
				.blobName(fileName)
				.sasToken(sasToken)
				.buildClient();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		blob.download(outputStream);
		final byte[] bytes = outputStream.toByteArray();
		return bytes;
	}


}


//
//
//
//	// Download file from Azure Blob Storage
//	public byte[] getFile(String fileName) throws URISyntaxException {
//
//		BlobClient blob = blobContainerClient.getBlobClient(fileName);
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//		blob.download(outputStream);
//		final byte[] bytes = outputStream.toByteArray();
//		return bytes;
//
//	}
//
//	// Read file in browser or download
//	public void readFileOrDownload(String fileName) throws IOException, URISyntaxException {
//		byte[] fileBytes = getFile(fileName);
//		if (fileBytes != null && fileBytes.length > 0) {
//			// Check if the browser supports inline display
//			String userAgent = request.getHeader("User-Agent"); // import javax.servlet.http.HttpServletRequest;
//			boolean isBrowser = (userAgent != null && userAgent.contains("Mozilla"));
//			if (isBrowser) {
//				// Display the file in the browser
//				// Import javax.servlet.http.HttpServletResponse
//				response.setContentType("application/pdf"); // Set the appropriate content type
//				response.setContentLength(fileBytes.length); // Set the content length
//				response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\""); // Set the content disposition
//				response.getOutputStream().write(fileBytes); // Write the file bytes to the response output stream
//				response.getOutputStream().flush();
//			} else {
//				// Download the file
//				response.setContentType("application/octet-stream"); // Set the appropriate content type
//				response.setContentLength(fileBytes.length); // Set the content length
//				response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\""); // Set the content disposition
//				response.getOutputStream().write(fileBytes); // Write the file bytes to the response output stream
//				response.getOutputStream().flush();
//			}
//		}
//	}
//


//
//
//	// List all files in Azure Blob Storage
//	public List<String> listBlobs() {
//
//		PagedIterable<BlobItem> items = blobContainerClient.listBlobs();
//		List<String> names = new ArrayList<String>();
//		for (BlobItem item : items) {
//			names.add(item.getName());
//		}
//		return names;
//
//	}

//

//
