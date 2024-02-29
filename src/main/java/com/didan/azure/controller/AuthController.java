package com.didan.azure.controller;

import com.didan.azure.entity.Users;
import com.didan.azure.payload.ResponseData;
import com.didan.azure.service.impl.AuthServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthServiceImpl authService;
    @Autowired
    public AuthController(AuthServiceImpl authService){
        this.authService = authService;
    }

    @PostMapping(value = "/signin", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postLogin(@RequestParam String username, @RequestParam String password){
        ResponseData payload = new ResponseData();
        Map<String, String> response = new HashMap<>();
        try{
            Users user = authService.login(username, password);
            if (user != null) {
                payload.setDescription("Login Successful");
                response.put("userId", user.getUserId());
                response.put("accessToken", user.getAccessToken());
                payload.setData(response);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @PostMapping(value = "/signup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> postRegister(@RequestParam String username, @RequestParam String password){
        ResponseData payload = new ResponseData();
        Map<String, String> response = new HashMap<>();
        try{
            Users user = authService.register(username, password);
            if (user != null) {
                payload.setDescription("Register Successful");
                response.put("userId", user.getUserId());
                response.put("accessToken", user.getAccessToken());
                payload.setData(response);
            }
            return new ResponseEntity<>(payload, HttpStatus.OK);
        } catch (Exception e){
            payload.setDescription(e.getMessage());
            payload.setStatusCode(500);
            payload.setSuccess(false);
            return new ResponseEntity<>(payload, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
