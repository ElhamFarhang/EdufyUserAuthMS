package com.example.edufyuserauthms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

//--------------------- Elham - UserDTO --------------
@Data
@NoArgsConstructor
public class UserDTO {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
}