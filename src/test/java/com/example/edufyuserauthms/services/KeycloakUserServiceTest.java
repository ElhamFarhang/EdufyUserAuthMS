package com.example.edufyuserauthms.services;

import com.example.edufyuserauthms.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

//--------------------- Elham - KeycloakUserServiceTest --------------
@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

    @Mock
    private KeycloakAuthService keycloakAuthServiceMock;

    @Mock
    private RestTemplate restTemplateMock;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(keycloakUserService, "restTemplate", restTemplateMock);
        ReflectionTestUtils.setField(keycloakUserService, "keycloakBaseUrl", "http://keycloak:8080");
        ReflectionTestUtils.setField(keycloakUserService, "realm", "edufy-realm");
        ReflectionTestUtils.setField(keycloakUserService, "roleId", "d37ebe43-4496-4268-8fd7-aa5ddd86cc28");
        ReflectionTestUtils.setField(keycloakUserService, "roleName", "edufy_User");
    }

    @Test
    void createUser_ShouldCreateUserInKeycloak() {
        //Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("newUsername");
        userDTO.setPassword("newPassword");
        userDTO.setEmail("newuser@mail.com");
        userDTO.setFirstName("newFirstName");
        userDTO.setLastName("newLastName");

        when(keycloakAuthServiceMock.getAccessToken()).thenReturn("admin-token");
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create("keycloakBaseUrl/admin/realms/realm/users/12345"));
        ResponseEntity<Void> response = new ResponseEntity<>(headers, HttpStatus.CREATED);

        when(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class))).thenReturn(response);
        //Act
        String userId = keycloakUserService.createUser(userDTO);
        //Assert
        assertEquals("12345", userId);
    }

    @Test
    void createUser_ShouldReturnFailureMessageWhenUserCreationFails() {
        //Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("failuser");
        userDTO.setPassword("password");

        when(keycloakAuthServiceMock.getAccessToken()).thenReturn("admin-token");
        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        when(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class))).thenReturn(response);
        //Act
        String result = keycloakUserService.createUser(userDTO);
        //Assert
        assertTrue(result.contains("Failed to create user in Keycloak"));
    }

    @Test
    void createUser_ShouldThrowExceptionWhenPostFails() {
        //Arrange
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("erroruser");
        userDTO.setPassword("password123");

        when(keycloakAuthServiceMock.getAccessToken()).thenReturn("admin-token");

        when(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Connection error"));
        //Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> keycloakUserService.createUser(userDTO));
        assertTrue(ex.getMessage().contains("Error creating user in Keycloak"));
    }

    @Test
    void assignRole_ShouldAssignUserRole() {
        //Arrange
        String userId = "12345";
        when(keycloakAuthServiceMock.getAccessToken()).thenReturn("admin-token");

        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
        when(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(response);
        //Act & Assert
        assertDoesNotThrow(() -> keycloakUserService.assignRole(userId));
        verify(restTemplateMock, times(1)).postForEntity(contains(userId), any(HttpEntity.class), eq(Void.class));
    }

    @Test
    void assignRole_ShouldThrowExceptionWhenAssignUserRoleFails() {
        //Arrange
        String userId = "12345";
        when(keycloakAuthServiceMock.getAccessToken()).thenReturn("admin-token");

        when(restTemplateMock.postForEntity(anyString(), any(HttpEntity.class), eq(Void.class)))
                .thenThrow(new RuntimeException("Connection error"));
        //Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> keycloakUserService.assignRole(userId));
        assertTrue(ex.getMessage().contains("Error assigning role"));
    }
}