package com.didan.azure.entity;

import jakarta.persistence.*;

import java.util.Date;
import java.util.Set;

@Entity(name = "file_info")
public class FileInfo {
    @Id
    @Column(name = "file_id", unique = true)
    private String fileId;

    @Column(name = "sas_token", nullable = false, unique = true)
    private String sasToken;

    @Column(name = "expired_at", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiredAt;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @ManyToOne
    @JoinColumn(name = "own_file", nullable = false)
    private Users users;

    @OneToMany(mappedBy = "fileInfo", cascade = CascadeType.ALL)
    private Set<Sas> sass;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getSasToken() {
        return sasToken;
    }

    public void setSasToken(String sasToken) {
        this.sasToken = sasToken;
    }

    public Date getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Date expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    public Set<Sas> getSass() {
        return sass;
    }

    public void setSass(Set<Sas> sass) {
        this.sass = sass;
    }
}
