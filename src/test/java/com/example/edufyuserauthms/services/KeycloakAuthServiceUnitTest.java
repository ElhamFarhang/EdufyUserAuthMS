package com.example.edufyuserauthms.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

//--------------------- Elham - KeycloakAuthServiceUnitTest --------------
@ExtendWith(MockitoExtension.class)
class KeycloakAuthServiceUnitTest {

    @Mock
    private RestTemplate restTemplateMock;
    private KeycloakAuthService keycloakAuthService = new KeycloakAuthService();;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakAuthService, "restTemplate", restTemplateMock);
    }

    @Test
    void getAccessToken_ShouldReturnAccessToken() {
        //Arrange
        Map<String, Object> response  = new HashMap<>();
        response.put("access_token", "mockToken");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);
        when(restTemplateMock.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(responseEntity);
        //Act
        String accessToken = keycloakAuthService.getAccessToken();
        //Assert
        assertEquals("mockToken", accessToken);
    }

    @Test
    void testGetAccessToken_ShouldThrowsExceptionWhenAccessTokenMissing() {
        //Arrange
        ResponseEntity<Map> response = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        when(restTemplateMock.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(),
                eq(Map.class)
        )).thenReturn(response);
        //Act & Assert
        assertThrows(RuntimeException.class, () -> keycloakAuthService.getAccessToken());
    }

    @Test
    void extractSub_ShouldReturnSub() throws Exception {
        //Arrange
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("signedUser").build();
        byte[] secret = new byte[32];
        Arrays.fill(secret, (byte) 1);
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWSObject jws = new JWSObject(header, new Payload(claims.toJSONObject()));
        JWSSigner signer = new MACSigner(secret);
        jws.sign(signer);
        String token = jws.serialize();
        KeycloakAuthService service = new KeycloakAuthService();
        //Act
        String sub = service.extractSub(token);
        //Assert
        assertEquals("signedUser", sub);
    }

    @Test
    void extractSub_ShouldThrowExceptionWhenTokenIsInvalid() {
        //Act & Assert
        assertThrows(RuntimeException.class, () -> keycloakAuthService.extractSub("invalidToken"));
    }
}