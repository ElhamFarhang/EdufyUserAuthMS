package com.example.edufyuserauthms.controllers;

import com.example.edufyuserauthms.dto.LoginRequestDTO;
import com.example.edufyuserauthms.dto.LogoutRequestDTO;
import com.example.edufyuserauthms.services.KeycloakLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

//--------------------- Elham - LoginController --------------
@RestController
@RequestMapping("/edufy/user")
public class LoginController {

    private final KeycloakLoginService keycloakLoginService;

    @Autowired
    public LoginController(KeycloakLoginService keycloakLoginService) {
        this.keycloakLoginService = keycloakLoginService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequestDTO dto) {
        System.out.println("Received DTO: " + dto);
        Map<String, Object> tokens = keycloakLoginService.login(dto);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody LogoutRequestDTO request) {
        String result = keycloakLoginService.logout(request.getEmail());
        return ResponseEntity.ok(result);
    }

}

