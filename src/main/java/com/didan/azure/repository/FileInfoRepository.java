package com.didan.azure.repository;

import com.didan.azure.entity.FileInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileInfoRepository extends JpaRepository<FileInfo, String> {
    FileInfo findByFileName(String fileName);
    FileInfo findBySasToken(String sasToken);
    List<FileInfo> findByUsers_UserId(String userId);
    FileInfo findFirstByFileNameAndUsers_UserId(String fileName, String userId);
}
