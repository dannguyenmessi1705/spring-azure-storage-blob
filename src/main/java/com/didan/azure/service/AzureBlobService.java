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
	public List<String> upload(MultipartFile[] multipartFiles) throws IOException, AzureBlobStorageException {
		String accessToken = jwtUtils.getTokenFromHeader(request);
		if (!StringUtils.hasText(accessToken)) {
			logger.info("Not Authorized");
			throw new AzureBlobStorageException("Not Authorized");
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
				throw new AzureBlobStorageException("Not Authorized");
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
			Sas sasKey = sasRepository.findBySasId_OwnPermisSasAndSasId_SasToken(shareUser.getUserId(), file.getSasToken());
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
				throw new AzureBlobStorageException("Not Authorized");
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
				throw new AzureBlobStorageException("Not Authorized");
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
				throw new AzureBlobStorageException("Not Authorized");
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
			throw new AzureBlobStorageException("Not Authorized");
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
			Sas sas = sasRepository.findBySasId_OwnPermisSasAndSasId_SasToken(user.getUserId(), file.getSasToken());
			if (sas == null) {
				throw new AzureBlobStorageException("Not authorized to download this file");
			}
			sasToken = sas.getSasId().getSasToken();
		}
		BlobClient blob = new BlobClientBuilder()
				.endpoint(endpoint)
				.containerName(file.getUsers().getUsername())
				.blobName(fileName)
				.sasToken(sasToken)
				.buildClient();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		blob.download(outputStream);
		final byte[] bytes = outputStream.toByteArray();
		return bytes;
	}

	@Override
	public Map<String, Map<String, String>> listFiles() throws URISyntaxException, AzureBlobStorageException {
		try{
			Map<String, Map<String, String>> list = new HashMap<>();
			String accessToken = jwtUtils.getTokenFromHeader(request);
			if (!StringUtils.hasText(accessToken)) {
				logger.info("Not Authorized");
				throw new AzureBlobStorageException("Not Authorized");
			}
			String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
			Users user = userRepository.findFirstByUserId(userId);
			if (user == null) {
				throw new AzureBlobStorageException("User not found");
			}
			List<FileInfo> files = fileInfoRepository.findByUsers_UserId(userId);
			List<Sas> fileShareds = sasRepository.findBySasId_OwnPermisSas(userId);
			if (files.isEmpty() && fileShareds.isEmpty()) {
				throw new AzureBlobStorageException("Your storage is empty");
			}
			String protocol = request.getScheme();
			String host = request.getHeader("host");
			String http = protocol + "://" + host + "/download?fileName=";
			if (!files.isEmpty()){
				Map<String, String> ownFile = new HashMap<>();
				for (FileInfo file : files) {
					ownFile.put(file.getFileName(), http + file.getFileName());
				}
				list.put("own files", ownFile);
			}
			if (!fileShareds.isEmpty()){
				Map<String, String> sharedFile = new HashMap<>();
				for (Sas fileShared : fileShareds) {
					FileInfo fileSharedFileInfo = fileInfoRepository.findBySasToken(fileShared.getSasId().getSasToken());
					sharedFile.put(fileSharedFileInfo.getFileName(), http + fileSharedFileInfo.getFileName());
				}
				list.put("shared files", sharedFile);
			}
			return list;
		} catch (Exception e){
			throw new AzureBlobStorageException(e.getMessage());
		}
	}
}
