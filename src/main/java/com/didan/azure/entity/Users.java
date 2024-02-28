package com.didan.azure.entity;

import jakarta.persistence.*;

import java.util.Date;
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

    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @OneToMany(mappedBy = "infoSas", cascade = CascadeType.ALL)
    private Set<InfoSass> infoSass;

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

    public Set<InfoSass> getInfoSass() {
        return infoSass;
    }

    public void setInfoSass(Set<InfoSass> infoSass) {
        this.infoSass = infoSass;
    }
}