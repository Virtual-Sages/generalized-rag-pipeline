package com.genrag.user.internal;

import com.genrag.user.api.UserDto;

final class UserMapper {
    private UserMapper() {}

    static UserDto toDto(UserEntity entity) {
        return new UserDto(entity.getId(), entity.getUsername(), entity.getEmail());
    }
}
