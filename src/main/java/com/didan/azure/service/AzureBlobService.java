package com.didan.azure.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClientBuilder;
import com.didan.azure.entity.FileInfo;
import com.didan.azure.entity.Users;
import com.didan.azure.exception.AzureBlobStorageException;
import com.didan.azure.repository.FileInfoRepository;
import com.didan.azure.repository.UserRepository;
import com.didan.azure.service.impl.AzureBlobServiceImpl;
import com.didan.azure.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
	@Autowired
	public AzureBlobService(HttpServletRequest request,
							HttpServletResponse response,
							BlobServiceClient blobServiceClient,
							JwtUtils jwtUtils,
							UserRepository userRepository,
							FileInfoRepository fileInfoRepository){
		this.request = request;
		this.response = response;
		this.blobServiceClient = blobServiceClient;
		this.jwtUtils = jwtUtils;
		this.userRepository = userRepository;
		this.fileInfoRepository = fileInfoRepository;
	}

	// Upload file to Azure Blob Storage
	@Override
	public String uploadFile(MultipartFile multipartFile) throws IOException {
		String accessToken = jwtUtils.getTokenFromHeader(request);
		if (!StringUtils.hasText(accessToken)) {
			logger.info("Not Authorized");
		}
		String userId = jwtUtils.getUserIdFromAccessToken(accessToken);
		Users user = userRepository.findFirstByUserId(userId);
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
		FileInfo fileInfo = new FileInfo();
		fileInfo.setFileId(UUID.randomUUID().toString());
		fileInfo.setFileName(newFileName);
		fileInfo.setFilePath(blobClient.getBlobUrl());
		fileInfo.setUsers(user);
		fileInfoRepository.save(fileInfo);
		return multipartFile.getOriginalFilename();
	}

//	// Upload many files to Azure Blob Storage
//	public List<String> uploadMany(MultipartFile[] multipartFiles) throws IOException {
//		List<String> fileNames = new ArrayList<String>();
//		for (MultipartFile multipartFile : multipartFiles) {
//			BlobClient blob = blobContainerClient.getBlobClient(multipartFile.getOriginalFilename());
//			blob.upload(multipartFile.getInputStream(), multipartFile.getSize(), true);
//			fileNames.add(multipartFile.getOriginalFilename());
//		}
//		return fileNames;
//	}
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
//	// Create a new container in Azure Blob Storage
//	public Boolean createContainer(String containerName) {
//		// find out if the container exists
//		if (blobServiceClient.getBlobContainerClient(containerName).exists()) {
//			return false;
//		}
//		blobServiceClient.createBlobContainer(containerName);
//		return true;
//	}
//
//	// Delete a container in Azure Blob Storage
//	public Boolean deleteContainer(String containerName) {
//		if (!blobServiceClient.getBlobContainerClient(containerName).exists()) {
//			return false;
//		}
//		blobServiceClient.deleteBlobContainer(containerName);
//		return true;
//	}
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
//	// Delete file from Azure Blob Storage
//	public Boolean deleteBlob(String blobName) {
//
//		BlobClient blob = blobContainerClient.getBlobClient(blobName);
//		blob.delete();
//		return true;
//	}
//
//	// Delete many files from Azure Blob Storage
//	public Boolean deleteManyBlobs(String[] blobNames) {
//		for (String blobName : blobNames) {
//			// if the blob does not exist, it will throw an exception
//			if (blobContainerClient.getBlobClient(blobName).exists()) {
//				BlobClient blob = blobContainerClient.getBlobClient(blobName);
//				blob.delete();
//			}
//		}
//		return true;
//	}
//
//	// Delete all files from Azure Blob Storage
//	public Boolean deleteAllBlobs() {
//		PagedIterable<BlobItem> items = blobContainerClient.listBlobs();
//		for (BlobItem item : items) {
//			BlobClient blob = blobContainerClient.getBlobClient(item.getName());
//			blob.delete();
//		}
//		return true;
//	}

}
