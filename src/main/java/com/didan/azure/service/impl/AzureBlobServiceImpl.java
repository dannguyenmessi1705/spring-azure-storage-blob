package com.didan.azure.service.impl;

import com.didan.azure.exception.AzureBlobStorageException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AzureBlobServiceImpl {
    List<String> upload(MultipartFile[] files) throws IOException;
    boolean shareFile(String fileName, String username) throws AzureBlobStorageException;
}
