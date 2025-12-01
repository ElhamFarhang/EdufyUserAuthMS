package com.example.edufyuserauthms.services;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

//--------------------- Elham - KeycloakAuthService --------------
@Service
public class KeycloakAuthService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.client.id}")
    private String adminClientId;

    @Value("${keycloak.admin.client.secret}")
    private String adminClientSecret;

    public String getAccessToken() {
        String url = keycloakBaseUrl + "/realms/" + realm +"/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String body = "grant_type=client_credentials"+
                "&client_id="+ adminClientId +
                "&client_secret="+ adminClientSecret;
        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("access_token")) {
            throw new RuntimeException("Failed to retrieve access token");
        }
        return (String) response.getBody().get("access_token");
    }

    public String extractSub(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new RuntimeException("Could not extract sub", e);
        }
    }
}