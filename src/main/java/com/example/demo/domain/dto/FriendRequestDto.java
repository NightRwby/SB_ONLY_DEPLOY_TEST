package com.example.demo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 친구 요청 시, 요청 대상의 이메일을 담는 DTO입니다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDto {

    // 친구 요청을 받을 사용자의 이메일 (식별자로 사용)
    private String receiverEmail;
}