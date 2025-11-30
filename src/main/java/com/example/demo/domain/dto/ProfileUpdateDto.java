package com.example.demo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateDto {

    private MultipartFile profileImageUrl;  //새 이미지(선택)
    private String nickname;               //닉네임 설정 (선택)
    private boolean deleteBanner;            //이미지 삭제 체크(선택)
    private String stateMessage;
    private MultipartFile bannerImageUrl;
}
