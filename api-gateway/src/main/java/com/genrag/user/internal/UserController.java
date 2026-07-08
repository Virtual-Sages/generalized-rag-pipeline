package com.genrag.user.internal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    // Dummy API
    // TODO: Cleanup when actual APIs are commited
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Success from UserController");
    }
}
