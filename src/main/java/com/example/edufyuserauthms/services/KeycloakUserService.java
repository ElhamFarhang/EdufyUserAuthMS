package com.example.edufyuserauthms.services;


import com.example.edufyuserauthms.dto.UserDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//--------------------- Elham - KeycloakUserService --------------
@Service
public class KeycloakUserService {

    private static final Logger FUNCTIONALITY_LOGGER = LogManager.getLogger("functionality");
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client.uuid.edufy}")
    private String targetClientUuId;

    @Value("${keycloak.role.id}")
    private String roleId;

    @Value("${keycloak.role.name}")
    private String roleName;

    private final KeycloakAuthService keycloakAuthService;

    public KeycloakUserService(KeycloakAuthService keycloakAuthService) {
        this.keycloakAuthService = keycloakAuthService;
    }

    public String createUser(UserDTO request) {
        String token = keycloakAuthService.getAccessToken();
        String url = keycloakBaseUrl + "/admin/realms/" + realm + "/users";

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", request.getPassword());
        credentials.put("temporary", false);

        Map<String, Object> body = new HashMap<>();
        body.put("enabled", true);
        body.put("username", request.getUsername());
        body.put("firstName", request.getFirstName());
        body.put("lastName", request.getLastName());
        body.put("email", request.getEmail());
        body.put("emailVerified", true);
        body.put("credentials", List.of(credentials));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType((MediaType.APPLICATION_JSON));
        headers.setBearerAuth(token);

        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, httpEntity, Void.class);
            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 201) {
                String location = response.getHeaders().getLocation().toString();
                String userId = location.substring(location.lastIndexOf("/") + 1);
                FUNCTIONALITY_LOGGER.info("User created successfully: username='{}', userId='{}'",request.getUsername(), userId);
                return userId;
            } else {
                FUNCTIONALITY_LOGGER.warn("Failed to create user: username='{}'", request.getUsername());
                return "Failed to create user in Keycloak. Status: " + response.getStatusCode();
            }
        } catch (Exception e) {
            FUNCTIONALITY_LOGGER.error("Exception while creating user: username='{}', error='{}'",
                    request.getUsername(), e.getMessage());
            throw new RuntimeException("Error creating user in Keycloak: " + e.getMessage(), e);
        }
    }

    public void assignRole(String userId) {

        String token = keycloakAuthService.getAccessToken();
        String roleUrl = keycloakBaseUrl + "/admin/realms/" + realm +
                "/users/" + userId + "/role-mappings/clients/" + targetClientUuId;

        Map<String, Object> roleBody = Map.of(
                "id", roleId,
                "name", roleName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<List<Map<String, Object>>> roleRequest = new HttpEntity<>(List.of(roleBody), headers);
        try {
            restTemplate.postForEntity(roleUrl, roleRequest, Void.class);
            FUNCTIONALITY_LOGGER.info("Role '{}' (ID: {}) successfully assigned to user '{}'",
                    roleName, roleId, userId);
        }catch (Exception e) {
            FUNCTIONALITY_LOGGER.error("Exception while assigning role '{}' to user '{}'. Error: {}",
                    roleName, userId, e.getMessage());
            throw new RuntimeException("Error assigning role: " + e.getMessage(), e);
        }
    }
}
