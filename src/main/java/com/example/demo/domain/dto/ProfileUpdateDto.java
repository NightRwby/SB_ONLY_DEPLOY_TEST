package com.example.demo.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ProfileUpdateDto {

    private MultipartFile profileImageUrl;  //새 이미지(선택)
    private String nickName;               //닉네임 설정 (선택)
    private boolean deleteImage;            //이미지 삭제 체크(선택)
}
