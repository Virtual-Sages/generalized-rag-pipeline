package com.genrag.auth.internal.ExceptionHandlers;

import java.util.List;
import java.util.Map;

import com.genrag.auth.internal.AuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
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
        List<FieldError> allFieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = "Bad request";

        boolean hasMissingField = allFieldErrors
            .stream()
            .anyMatch(err -> {
                    Object value = err.getRejectedValue();
                    return value == null || (value instanceof String fieldValue && fieldValue.isBlank());       // new way (check instance of -> assign it -> check is blank)
                }
            );

        if(hasMissingField) {
            errorMessage = "All the given fields are required";
        } else {
            errorMessage = allFieldErrors
                .stream()
                .filter(err -> err.getDefaultMessage() != null)
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .findFirst()
                .orElse(errorMessage);
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", errorMessage));
    }
}
