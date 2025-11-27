package com.example.demo.controller;


import com.example.demo.domain.dto.Inquiry_Dto;
import com.example.demo.domain.entity.InquiryEntity;
import com.example.demo.domain.service.InquiryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/inquiry")
public class InquiryController {

    private final InquiryService inquiryService;

    public InquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    // 문의 목록 조회
    @GetMapping
    public String listInquiries(Model model) {
        List<InquiryEntity> inquiries = inquiryService.getAllInquiries();
        model.addAttribute("inquiries", inquiries);
        return "/inquiry";
    }

    // 문의 등록 폼 페이지 (수정)
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("inquiry", new Inquiry_Dto());
        return "inquiry_create";
    }

    // 문의 등록 처리
    @PostMapping
    public String createInquiry(
            @Valid
            @ModelAttribute("inquiry") Inquiry_Dto inquiryDto,
            BindingResult bindingResult,
            Model model) {
        //유효성 검증 실패 여부
        if (bindingResult.hasErrors()) {
            return "inquiry_create";
        }
        //유효성 검증 통과 여부
        InquiryEntity inquiryEntity = new InquiryEntity();
        inquiryEntity.setInquiryType((inquiryDto.getInquiryType()));
        inquiryEntity.setTitle(inquiryDto.getTitle());
        inquiryEntity.setContent(inquiryDto.getContent());

        inquiryService.saveInquiry(inquiryEntity);
        return "redirect:/inquiry";
    }

    // 문의 수정 폼 페이지
    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        InquiryEntity inquiryEntity = inquiryService.getInquiryById(id);

        Inquiry_Dto inquiryDto = new Inquiry_Dto();
        inquiryDto.setInquiryType(inquiryEntity.getInquiryType());
        inquiryDto.setTitle(inquiryEntity.getTitle());
        inquiryDto.setContent(inquiryEntity.getContent());

        model.addAttribute("inquiry", inquiryDto);
        model.addAttribute("inquiryId", id);
        return "inquiry_update";
    }

    // 문의 수정 처리
    @PostMapping("/{id}/update")
    public String updateInquiry(@PathVariable Long id,
                                @Valid @ModelAttribute("inquiry") Inquiry_Dto inquiryDto,
                                BindingResult bindingResult,
                                Model model) {
        //검증 실패 여부
        if (bindingResult.hasErrors()) {
            model.addAttribute("inquiryId", id);
            return "inquiry_update";
        }
        //검증 통과 여부
        InquiryEntity updatedInquiryEntity = new InquiryEntity();

        updatedInquiryEntity.setInquiryType(inquiryDto.getInquiryType());
        updatedInquiryEntity.setTitle(inquiryDto.getTitle());
        updatedInquiryEntity.setContent(inquiryDto.getContent());

        // 3. 서비스 호출: DTO에서 변환한 Entity를 전달
        inquiryService.updateInquiry(id, updatedInquiryEntity);

        return "redirect:/inquiry/{id}";
    }

    // 문의 삭제
    @PostMapping("/{id}/delete")
    public String deleteInquiry(@PathVariable Long id) {
        inquiryService.deleteInquiry(id);
        return "redirect:/inquiry";
    }

    // 처리완료
    @PostMapping("/{id}/end")
    public String endInquiry(@PathVariable Long id) {
        inquiryService.endInquiry(id);
        return "redirect:/inquiry";
    }

    // 문의 상세 조회
    @GetMapping("/{id}")
    public String viewInquiry(@PathVariable Long id, Model model) {
        InquiryEntity inquiry = inquiryService.getInquiryById(id);
        model.addAttribute("inquiry", inquiry);
        return "inquiry_detail";
    }

    // 고객센터 안내 페이지
    @GetMapping("/center")
    public String showInquiryCenter() {
        return "inquiry_center"; // → templates/inquiry_center.html 로 이동
    }

}
