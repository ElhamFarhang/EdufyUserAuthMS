package com.example.edufyuserauthms.services;

import com.example.edufyuserauthms.dto.LoginRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

//--------------------- Elham - KeycloakLoginServiceUnitTest --------------
@ExtendWith(MockitoExtension.class)
class KeycloakLoginServiceUnitTest {

    @Mock
    private KeycloakAuthService keycloakAuthServiceMock;
    @Mock
    private RestTemplate restTemplateMock;

    @InjectMocks
    private KeycloakLoginService keycloakLoginService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakLoginService, "restTemplate", restTemplateMock);
    }

    @Test
    void login_ShouldReturnToken() {
        //Arrange
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("username");
        loginRequest.setPassword("password");

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "mockToken");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(response, HttpStatus.OK);

        when(restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(Map.class))).thenReturn(responseEntity);
        when(keycloakAuthServiceMock.extractSub("mockToken")).thenReturn("user1");
        //Act
        Map<String, Object> result = keycloakLoginService.login(loginRequest);
        //Assert
        assertNotNull(result);
        assertEquals("mockToken", result.get("access_token"));
    }

    @Test
    void login_ShouldThrowExceptionWhenKeycloakReturnsNonOkStatus() {
        //Arrange
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("invalid-username");
        loginRequest.setPassword("invalid-password");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(new HashMap<>(), HttpStatus.BAD_REQUEST);

        when(restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(Map.class))).thenReturn(responseEntity);
        //Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> keycloakLoginService.login(loginRequest));
        assertTrue(ex.getMessage().contains("Received non-OK status from Keycloak"));
    }

    @Test
    void login_ShouldThrowExceptionWhenRestTemplateFails() {
        //Arrange
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setUsername("username");
        loginRequest.setPassword("password");

        when(restTemplateMock.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(Map.class))).thenThrow(new RuntimeException("Connection error"));
        //Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> keycloakLoginService.login(loginRequest));
        assertTrue(ex.getMessage().contains("Error communicating with Keycloak"));
    }

    @Test
    void logout_ShouldForceLogoutWhenUserExists() {
        //Arrange
        String adminToken = "adminToken";
        String email = "user@mail.com";
        String userId = "12345";

        when(keycloakAuthServiceMock.getAccessToken()).thenReturn(adminToken);
        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        List<Map<String, Object>> users = List.of(user);
        ResponseEntity<List> userResponse = new ResponseEntity<>(users, HttpStatus.OK);

        when(restTemplateMock.exchange(
                contains("/users?email=" + email), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(List.class))).thenReturn(userResponse);
        ResponseEntity<Void> logoutResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);

        when(restTemplateMock.exchange(
                contains("/users/" + userId + "/logout"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Void.class))).thenReturn(logoutResponse);
        //Act
        String result = keycloakLoginService.logout(email);
        //Assert
        assertEquals("User " + email + " has been logged out successfully", result);
    }

    @Test
    void logout_ShouldReturnWarningMessageWhenUserNotFound() {
        //Arrange
        String email = "invalidUser@mail.com";
        when(keycloakAuthServiceMock.getAccessToken()).thenReturn("admin-token");
        ResponseEntity<List> emptyResponse = new ResponseEntity<>(List.of(), HttpStatus.OK);

        when(restTemplateMock.exchange(
                contains("/users?email=" + email), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(List.class))).thenReturn(emptyResponse);
        //Act
        String result = keycloakLoginService.logout(email);
        //Assert
        assertEquals("No user found with email: " + email, result);
    }

    @Test
    void logout_ShouldReturnStatusWarningWhenLogoutFailsWithNon2xx() {
        //Arrange
        String email = "user@mail.com";
        String userId = "12345";
        String adminToken = "adminToken";

        when(keycloakAuthServiceMock.getAccessToken()).thenReturn(adminToken);
        Map<String, Object> user = new HashMap<>();
        user.put("id", userId);
        List<Map<String, Object>> users = List.of(user);
        ResponseEntity<List> userResponse = new ResponseEntity<>(users, HttpStatus.OK);
        when(restTemplateMock.exchange(
                contains("/users?email=" + email), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(List.class))).thenReturn(userResponse);

        ResponseEntity<Void> logoutResponse = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        when(restTemplateMock.exchange(
                contains("/users/" + userId + "/logout"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Void.class))).thenReturn(logoutResponse);
        //Act
        String result = keycloakLoginService.logout(email);
        //Assert
        assertEquals("Logout request sent but received status: 400 BAD_REQUEST", result);
    }

    @Test
    void logout_ShouldReturnFailureMessageWhenExceptionOccurs() {
        //Arrange
        String email = "invalidUser@mail.com";
        when(keycloakAuthServiceMock.getAccessToken()).thenReturn("admin-token");
        Map<String, Object> user = new HashMap<>();
        user.put("id", "999");
        ResponseEntity<List> userResponse = new ResponseEntity<>(List.of(user), HttpStatus.OK);

        when(restTemplateMock.exchange(
                contains("/users?email=" + email), eq(HttpMethod.GET),
                any(HttpEntity.class), eq(List.class))).thenReturn(userResponse);

        when(restTemplateMock.exchange(
                contains("/logout"), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(Void.class))).thenThrow(new RuntimeException("Logout error"));
        //Act
        String result = keycloakLoginService.logout(email);
        //Assert
        assertTrue(result.contains("Logout failed"));
    }
}