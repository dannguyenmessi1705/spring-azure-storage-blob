package com.didan.azure.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;

@Component
public class AzureBlobService {

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final BlobContainerClient blobContainerClient;
	private final BlobServiceClient blobServiceClient;

	@Autowired
	public AzureBlobService(HttpServletRequest request, HttpServletResponse response, BlobServiceClient blobServiceClient, BlobContainerClient blobContainerClient){
		this.request = request;
		this.response = response;
		this.blobContainerClient = blobContainerClient;
		this.blobServiceClient = blobServiceClient;
	}

	// Upload file to Azure Blob Storage
	public String upload(MultipartFile multipartFile) throws IOException {
		// Todo UUID
		BlobClient blob = blobContainerClient.getBlobClient(multipartFile.getOriginalFilename());
		blob.upload(multipartFile.getInputStream(), multipartFile.getSize(), true);

		return multipartFile.getOriginalFilename();
	}

	// Upload many files to Azure Blob Storage
	public List<String> uploadMany(MultipartFile[] multipartFiles) throws IOException {
		List<String> fileNames = new ArrayList<String>();
		for (MultipartFile multipartFile : multipartFiles) {
			BlobClient blob = blobContainerClient.getBlobClient(multipartFile.getOriginalFilename());
			blob.upload(multipartFile.getInputStream(), multipartFile.getSize(), true);
			fileNames.add(multipartFile.getOriginalFilename());
		}
		return fileNames;
	}



	// Download file from Azure Blob Storage
	public byte[] getFile(String fileName) throws URISyntaxException {

		BlobClient blob = blobContainerClient.getBlobClient(fileName);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		blob.download(outputStream);
		final byte[] bytes = outputStream.toByteArray();
		return bytes;

	}

	// Read file in browser or download
	public void readFileOrDownload(String fileName) throws IOException, URISyntaxException {
		byte[] fileBytes = getFile(fileName);
		if (fileBytes != null && fileBytes.length > 0) {
			// Check if the browser supports inline display
			String userAgent = request.getHeader("User-Agent"); // import javax.servlet.http.HttpServletRequest;
			boolean isBrowser = (userAgent != null && userAgent.contains("Mozilla"));
			if (isBrowser) {
				// Display the file in the browser
				// Import javax.servlet.http.HttpServletResponse
				response.setContentType("application/pdf"); // Set the appropriate content type
				response.setContentLength(fileBytes.length); // Set the content length
				response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\""); // Set the content disposition
				response.getOutputStream().write(fileBytes); // Write the file bytes to the response output stream
				response.getOutputStream().flush();
			} else {
				// Download the file
				response.setContentType("application/octet-stream"); // Set the appropriate content type
				response.setContentLength(fileBytes.length); // Set the content length
				response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\""); // Set the content disposition
				response.getOutputStream().write(fileBytes); // Write the file bytes to the response output stream
				response.getOutputStream().flush();
			}
		}
	}

	// Create a new container in Azure Blob Storage
	public Boolean createContainer(String containerName) {
		// find out if the container exists
		if (blobServiceClient.getBlobContainerClient(containerName).exists()) {
			return false;
		}
		blobServiceClient.createBlobContainer(containerName);
		return true;
	}
	
	// Delete a container in Azure Blob Storage
	public Boolean deleteContainer(String containerName) {
		if (!blobServiceClient.getBlobContainerClient(containerName).exists()) {
			return false;
		}
		blobServiceClient.deleteBlobContainer(containerName);
		return true;
	}


	// List all files in Azure Blob Storage
	public List<String> listBlobs() {

		PagedIterable<BlobItem> items = blobContainerClient.listBlobs();
		List<String> names = new ArrayList<String>();
		for (BlobItem item : items) {
			names.add(item.getName());
		}
		return names;

	}

	// Delete file from Azure Blob Storage
	public Boolean deleteBlob(String blobName) {

		BlobClient blob = blobContainerClient.getBlobClient(blobName);
		blob.delete();
		return true;
	}

	// Delete many files from Azure Blob Storage
	public Boolean deleteManyBlobs(String[] blobNames) {
		for (String blobName : blobNames) {
			// if the blob does not exist, it will throw an exception
			if (blobContainerClient.getBlobClient(blobName).exists()) {
				BlobClient blob = blobContainerClient.getBlobClient(blobName);
				blob.delete();
			}
		}
		return true;
	}

	// Delete all files from Azure Blob Storage
	public Boolean deleteAllBlobs() {
		PagedIterable<BlobItem> items = blobContainerClient.listBlobs();
		for (BlobItem item : items) {
			BlobClient blob = blobContainerClient.getBlobClient(item.getName());
			blob.delete();
		}
		return true;
	}

}
