package com.genrag.user.internal;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genrag.user.api.UserDto;
import com.genrag.user.api.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the authenticated caller's profile.
     *
     * The user is resolved from the JWT principal populated by JwtAuthFilter,
     * never from the request, so a caller can only ever read their own record.
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @AuthenticationPrincipal(errorOnInvalidType = true) UUID userId) {
        try {
            return ResponseEntity.ok(userService.findById(userId));
        } catch (Exception e) {
            throw new RuntimeException("Error fetching current user profile: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the profile at {id}, which is only ever the caller's own.
     *
     * Reading another user's profile is not supported yet, so a mismatch is
     * rejected rather than silently serving the caller's record: the URI has to
     * stay truthful about what it returns. The lookup still uses the principal,
     * not the path, so the token remains the only source of identity.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(
            @PathVariable String id,
            @AuthenticationPrincipal(errorOnInvalidType = true) UUID userId) {
        try {
            if (!userId.toString().equalsIgnoreCase(id)) {
                throw new AccessDeniedException("Cannot access another user's profile");
            }
            return ResponseEntity.ok(userService.findById(UUID.fromString(id)));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching user profile for id " + id + ": " + e.getMessage(), e);
        }
    }
}
