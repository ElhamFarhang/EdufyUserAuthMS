package com.example.edufyuserauthms.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

//--------------------- Elham - LoginRequestDTO --------------
@Data
@NoArgsConstructor
public class LoginRequestDTO {
    private String username;
    private String password;
}

