package com.didan.azure.entity;

import jakarta.persistence.*;

import java.util.Set;

@Entity(name = "users")
public class Users {
    @Id
    @Column(name = "user_id", unique = true)
    private String userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "access_token", nullable = true)
    private String accessToken;

    @Column(name = "sas_directory", nullable = false)
    private String sasDirectory;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL)
    private Set<FileInfo> fileInfos;

    @OneToMany(mappedBy = "users", cascade = CascadeType.ALL)
    private Set<Sas> sass;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSasDirectory() {
        return sasDirectory;
    }

    public void setSasDirectory(String sasDirectory) {
        this.sasDirectory = sasDirectory;
    }

    public Set<FileInfo> getFileInfos() {
        return fileInfos;
    }

    public void setFileInfos(Set<FileInfo> fileInfos) {
        this.fileInfos = fileInfos;
    }

    public Set<Sas> getSass() {
        return sass;
    }

    public void setSass(Set<Sas> sass) {
        this.sass = sass;
    }
}