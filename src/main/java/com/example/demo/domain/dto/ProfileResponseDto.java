package com.example.demo.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponseDto {

    private String nickname;
    private String stateMessage;
    private String profileImageUrl;
    private String bannerImageUrl;

}
