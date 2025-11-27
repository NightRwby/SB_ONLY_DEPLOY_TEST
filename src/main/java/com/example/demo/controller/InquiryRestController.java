package com.example.demo.controller;


import com.example.demo.domain.entity.InquiryEntity;
import com.example.demo.domain.service.InquiryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries") // 경로 변경
public class InquiryRestController {

    private final InquiryService inquiryService;

    public InquiryRestController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    // 문의 목록 조회
    @GetMapping
    public List<InquiryEntity> listInquiries() {
        return inquiryService.getAllInquiries();
    }

    // 문의 등록
    @PostMapping
    public InquiryEntity createInquiry(@RequestBody InquiryEntity inquiry) {
        return inquiryService.saveInquiry(inquiry);
    }

    // 문의 수정
    @PutMapping("/{id}")
    public InquiryEntity updateInquiry(@PathVariable Long id, @RequestBody InquiryEntity updatedInquiry) {
        return inquiryService.updateInquiry(id, updatedInquiry);
    }

    // 문의 삭제
    @DeleteMapping("/{id}")
    public void deleteInquiry(@PathVariable Long id) {
        inquiryService.deleteInquiry(id);
    }

    //처리 완료
    @PutMapping("/{id}/end")
    public InquiryEntity endInquiry(@PathVariable Long id) {
        return inquiryService.endInquiry(id);
    }
}
