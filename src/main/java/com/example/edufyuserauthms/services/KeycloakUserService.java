package com.example.edufyuserauthms.services;


import com.example.edufyuserauthms.dto.UserDTO;
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

@Service
public class KeycloakUserService {

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
        ResponseEntity<Void> response = restTemplate.postForEntity(url, httpEntity, Void.class);
        if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 201) {
            String location = response.getHeaders().getLocation().toString();
            if (location == null) {
                throw new RuntimeException("Failed to create user: Location header missing");
            }
            String userId = location.substring(location.lastIndexOf("/") + 1);

            return userId;
        } else {
            throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatusCode());
        }
    }

    public void assignRole(String userId) {

        String token = keycloakAuthService.getAccessToken();
        String roleUrl = keycloakBaseUrl + "/admin/realms/" + realm +
                "/users/" + userId + "/role-mappings/clients/" + targetClientUuId;

        Map<String, Object> roleBody = Map.of(
                "id", roleId,
                "name", roleName
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<List<Map<String, Object>>> roleRequest = new HttpEntity<>(List.of(roleBody), headers);
        restTemplate.postForEntity(roleUrl, roleRequest, Void.class);

    }
}
