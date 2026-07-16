package com.genrag.user.api;

import java.util.UUID;

public record UserDto(UUID id, String username, String email) {}
