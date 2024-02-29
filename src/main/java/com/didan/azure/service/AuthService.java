package com.didan.azure.service;

import com.azure.storage.blob.sas.BlobSasPermission;
import com.didan.azure.entity.Users;
import com.didan.azure.repository.UserRepository;
import com.didan.azure.service.impl.AuthServiceImpl;
import com.didan.azure.utils.JwtUtils;
import com.didan.azure.utils.SasUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

@Service
public class AuthService implements AuthServiceImpl {
    @Value("${azure.storage.connection.string}")
    private String connectionString;
    private static Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final BlobServiceClient blobServiceClient;
    private SasUtils sasUtils;

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       BlobServiceClient blobServiceClient,
                       SasUtils sasUtils){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.blobServiceClient = blobServiceClient;
        this.sasUtils = sasUtils;
    }
    @Override
    public Users login(String username, String password) throws Exception {
        Users user = userRepository.findFirstByUsername(username);
        if (user == null){
            logger.error("username or password is incorrect");
            throw new Exception("username or password is incorrect");
        }
        if (!passwordEncoder.matches(password, user.getPassword())){
            logger.error("username or password is incorrect");
            throw new Exception("username or password is incorrect");
        }
        user.setAccessToken(jwtUtils.generateAccessToken(user.getUserId()));
        userRepository.save(user);
        return user;
    }

    @Override
    public Users register(String username, String password) throws Exception {
        Users user = userRepository.findFirstByUsername(username);
        if (user != null){
            logger.error("username is already existed");
            throw new Exception("username is already existed");
        }
        user = new Users();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setAccessToken(jwtUtils.generateAccessToken(user.getUserId()));
        // Tạo 1 container có tên là username và tạo SAS Token cho container đó
        if (blobServiceClient.getBlobContainerClient(username).exists()){
            logger.error("container is already existed"); // Kiểm tra xem container có tồn tại hay không
            throw new Exception("container is already existed"); // Nếu container đã tồn tại thì sẽ throw ra một Exception
        }
        blobServiceClient.createBlobContainer(username); // Tạo ra một container có tên là username
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(username);
        String sasDirectory = sasUtils.createServiceSASContainer(blobContainerClient); // Tạo ra một SAS Token cho container
        user.setSasDirectory(sasDirectory); // Set SAS Token cho thư mục vào user
        userRepository.save(user); // Lưu user vào database
        return user;
    }
}
