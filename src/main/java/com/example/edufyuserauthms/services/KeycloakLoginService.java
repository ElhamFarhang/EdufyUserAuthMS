package com.example.edufyuserauthms.services;


import com.example.edufyuserauthms.dto.LoginRequestDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

//--------------------- Elham - KeycloakLoginService --------------
@Service
public class KeycloakLoginService {

    private static final Logger FUNCTIONALITY_LOGGER = LogManager.getLogger("functionality");
    private final KeycloakAuthService keycloakAuthService;

    public KeycloakLoginService(KeycloakAuthService keycloakAuthService) {
        this.keycloakAuthService = keycloakAuthService;
    }

    private RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client.id.edufy}")
    private String targetClientId;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    public Map<String, Object> login(LoginRequestDTO loginRequest) {

        String url = keycloakBaseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "grant_type=password" +
                "&client_id=" + targetClientId +
                "&username=" + loginRequest.getUsername() +
                "&password=" + loginRequest.getPassword() +
                "&client_secret=" + clientSecret;
        HttpEntity< String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokens = response.getBody();
                String accessToken = (String) tokens.get("access_token");
                String sub = keycloakAuthService.extractSub(accessToken);
                FUNCTIONALITY_LOGGER.info("User with ID '{}' successfully logged in", sub);
                return response.getBody();
            } else {
                FUNCTIONALITY_LOGGER.warn("Login failed for username '{}'", loginRequest.getUsername());
                throw new RuntimeException("Error: Received non-OK status from Keycloak: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            FUNCTIONALITY_LOGGER.error("Login error for username '{}'", loginRequest.getUsername());
            throw new RuntimeException("Error communicating with Keycloak: " + e.getMessage(), e);
        }
    }

    public String logout(String email){

        String adminToken = keycloakAuthService.getAccessToken();
        String url = keycloakBaseUrl + "/admin/realms/" + realm + "/users?email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);

        if (response.getBody() == null || response.getBody().isEmpty()) {
            FUNCTIONALITY_LOGGER.warn("Logout attempt failed: No user found with email '{}'", email);
            return "No user found with email: " + email;
        }

        Map<String, Object> user = (Map<String, Object>) response.getBody().get(0);
        String userId = (String) user.get("id");

        String logoutUrl = keycloakBaseUrl + "/admin/realms/" + realm + "/users/" + userId + "/logout";

        try {
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(logoutUrl, HttpMethod.POST, entity, Void.class);
            if (logoutResponse.getStatusCode().is2xxSuccessful() || logoutResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                FUNCTIONALITY_LOGGER.info("User '{}' (ID: {}) logged out successfully", email, userId);
                return "User " + email + " has been logged out successfully";
            } else {
                FUNCTIONALITY_LOGGER.warn("Logout request sent for '{}' (ID: {}) but got status: {}", email, userId, logoutResponse.getStatusCode());
                return "Logout request sent but received status: " + logoutResponse.getStatusCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            FUNCTIONALITY_LOGGER.warn("Logout failed for '{}'", email);
            return "Logout failed. error: " + e.getMessage();
        }
    }
}

