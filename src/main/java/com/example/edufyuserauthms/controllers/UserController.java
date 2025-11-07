package com.example.edufyuserauthms.controllers;

import com.example.edufyuserauthms.dto.UserDTO;
import com.example.edufyuserauthms.services.KeycloakUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/newuser")
public class UserController {

    private final KeycloakUserService keycloakUserService;

    public UserController(KeycloakUserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody UserDTO request) {
        String userId = keycloakUserService.createUser(request);
        keycloakUserService.assignRole(userId);
        Map<String, String> response = Map.of(
                "userId", userId,
                "message", "User created and assigned role 'user'"
        );
        return ResponseEntity.ok(response);
    }
}
