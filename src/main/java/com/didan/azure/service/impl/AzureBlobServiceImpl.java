package com.didan.azure.service.impl;

import com.didan.azure.exception.AzureBlobStorageException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public interface AzureBlobServiceImpl {
    List<String> upload(MultipartFile[] files) throws IOException, AzureBlobStorageException;
    boolean shareFile(String fileName, String username) throws AzureBlobStorageException;
    boolean deleteFile(String fileName) throws AzureBlobStorageException;
    boolean deleteManyFiles(String[] blobNames) throws AzureBlobStorageException;
    boolean deleteAllFiles() throws AzureBlobStorageException;
    byte[] downloadFile(String fileName) throws IOException, AzureBlobStorageException;
    Map<String, Map<String, String>> listFiles() throws URISyntaxException, AzureBlobStorageException;
}
