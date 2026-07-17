package com.genrag.user.internal;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.genrag.user.api.exceptions.DuplicateUserException;
import com.genrag.user.api.exceptions.InvalidCredentialsException;
import com.genrag.user.api.UserDto;
import com.genrag.user.api.exceptions.UserNotFoundException;
import com.genrag.user.api.UserService;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserDto register(String username, String email, String rawPassword) {
        String password = passwordEncoder.encode(rawPassword);

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateUserException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateUserException("User with email already exists");
        }

        try {
            UserEntity saved = userRepository.saveAndFlush(new UserEntity(username, email, password));  // To avoid check then act race condition
            return UserMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateUserException("Username or email already exists");
        }
    }

    @Override
    public UserDto authenticate(String username, String rawPassword) {
        UserEntity user = userRepository
            .findByUsername(username)
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return UserMapper.toDto(user);
    }

    @Override
    public UserDto findById(UUID id) {
        return userRepository
            .findById(id)
            .map(UserMapper::toDto)
            .orElseThrow(UserNotFoundException::new);
    }
}
