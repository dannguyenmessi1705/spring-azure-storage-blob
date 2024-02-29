package com.didan.azure.service.impl;

import com.didan.azure.entity.Users;

public interface AuthServiceImpl {
    Users login(String username, String password) throws Exception;

    Users register(String username, String password) throws Exception;
}
