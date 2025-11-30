package com.example.demo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@Data
public class UserProfileDto {
    private Long id;
    private String email;
    private String nickName;
    private String profileImageUrl;
    private String stateMessage;
    private String onlineStatus;

    public UserProfileDto(Long id, String email, String nickName, String profileImageUrl, String stateMessage, String onlineStatus) {
        this.id = id;
        this.email = email;
        this.nickName = nickName;
        this.profileImageUrl = profileImageUrl;
        this.stateMessage = stateMessage;
        this.onlineStatus = onlineStatus;
    }

    public UserProfileDto(Long id, String email, String nickName) {
        this.id = id;
        this.email = email;
        this.nickName = nickName;
    }
}
