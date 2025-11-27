package com.example.demo.domain.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Inquiry_Dto {

//    문의 유형
    @NotBlank(message="문의 유형을 확인해주세요.")
    private String inquiryType;

//    문의 제목
    @NotBlank(message="문의 제목을 확인해주세요.")
    @Size(max = 25, message = "제목 최대 25자 입력")
    private String title;

//    문의 내용
    @NotBlank(message="문의 내용을 확인해주세요.")
    @Size(max = 500, message = "내용 최대 500자 입력")
    private String content;

}
