package com.didan.azure.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobContainerSasPermission;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class SasUtils {
    private final BlobServiceClient blobServiceClient;

    @Autowired
    public SasUtils(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = blobServiceClient;
    }

    public String generateSasTokenForContainer(String containerName) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        String containerPath = containerName;
        BlobClient blobClient = containerClient.getBlobClient(containerPath);
        SasProtocol sasProtocol = SasProtocol.HTTPS_HTTP;
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(OffsetDateTime.now().plusYears(5), BlobSasPermission.parse("racwdli"))
                .setVersion("2022-11-02")
                .setProtocol(sasProtocol)
                .setStartTime(OffsetDateTime.now());

        return blobClient.generateSas(sasValues);
    }
    public String createServiceSASContainer(BlobContainerClient containerClient) {
        // Create a SAS token that's valid for 1 day, as an example, Local UTC time
        OffsetDateTime expiryTime = OffsetDateTime.now().plusYears(5);

        // Assign read permissions to the SAS token
        BlobContainerSasPermission sasPermission = new BlobContainerSasPermission()
                .setCreatePermission(true)
                .setReadPermission(true);

        BlobServiceSasSignatureValues sasSignatureValues = new BlobServiceSasSignatureValues(expiryTime, sasPermission)
                .setStartTime(OffsetDateTime.now().minusMinutes(5));

        String sasToken = containerClient.generateSas(sasSignatureValues);
        return sasToken;
    }
}
