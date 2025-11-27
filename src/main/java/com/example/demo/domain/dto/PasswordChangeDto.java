package com.example.demo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeDto {

    private String email;  //사용자 식별자(현재 인증된 사용자)
    private String currentPassword;
    private String newPassword;

}
