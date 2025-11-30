package com.example.demo.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinDto {
    private String phoneNumber;
    @NotBlank(message = "비밀번호를 입력하세요.")
    private String passWord;
    @NotBlank(message = "필수 입력 항목입니다.")
    private String userName;
    @Email(message = "example@example.com 형식으로 입력하세요.")
    @NotBlank(message = "필수 입력 항목입니다.")
    private String email;
}
