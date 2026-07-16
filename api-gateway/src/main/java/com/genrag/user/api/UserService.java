package com.genrag.user.api;

import java.util.UUID;

public interface UserService {
    UserDto register(String username, String email, String rawPassword);
    UserDto authenticate(String username, String rawPassword);
    UserDto findById(UUID id);
}
