package com.didan.azure.service.impl;

import com.didan.azure.exception.AzureBlobStorageException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface AzureBlobServiceImpl {
    String uploadFile(MultipartFile file) throws IOException;
}
