package com.genrag.auth.internal.ExceptionHandlers;

import java.util.Map;

import com.genrag.auth.internal.AuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.genrag.user.api.exceptions.DuplicateUserException;
import com.genrag.user.api.exceptions.InvalidCredentialsException;
import com.genrag.user.api.exceptions.UserNotFoundException;

import io.jsonwebtoken.JwtException;

@RestControllerAdvice(assignableTypes = AuthController.class)
class AuthExceptionHandler {
    @ExceptionHandler(DuplicateUserException.class)
    ResponseEntity<Map<String, String>> handleDuplicate(DuplicateUserException e) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({
        InvalidCredentialsException.class,
        JwtException.class,
        UserNotFoundException.class
    })
    ResponseEntity<Map<String, String>> handleUnauthorized() {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", "Invalid credentials"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new java.util.HashMap<>();
        e
            .getBindingResult()
            .getFieldErrors()
            .forEach(
                fe -> fieldErrors.putIfAbsent(
                    fe.getField(), 
                    fe.getDefaultMessage()
                )
            );
        return ResponseEntity.badRequest().body(fieldErrors);
    }
}
