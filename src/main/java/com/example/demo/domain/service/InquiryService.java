package com.example.demo.domain.service;


import com.example.demo.domain.entity.InquiryEntity;
import com.example.demo.domain.repository.InquiryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    public InquiryService(InquiryRepository inquiryRepository) {
        this.inquiryRepository = inquiryRepository;
    }

    // 문의 등록 (Create)
    public InquiryEntity saveInquiry(InquiryEntity inquiry) {
        return inquiryRepository.save(inquiry);
    }

    // 전체 문의 조회 (Read All)
    public List<InquiryEntity> getAllInquiries() {
        return inquiryRepository.findAllByOrderByCreatedAtDesc();
    }

    //    단건조회
    public InquiryEntity getInquiryById(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의가 없습니다. id=" + id));
    }

    // 3 문의 수정 (Update)
    public InquiryEntity updateInquiry(Long id, InquiryEntity updatedInquiry) {
        InquiryEntity inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의가 없습니다. id=" + id));

        inquiry.setInquiryType(updatedInquiry.getInquiryType());
        inquiry.setTitle(updatedInquiry.getTitle());
        inquiry.setContent(updatedInquiry.getContent());

        return inquiryRepository.save(inquiry);
    }

    // 4 처리완료 (Update)
    public InquiryEntity endInquiry(Long id) {
        InquiryEntity inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 문의가 없습니다. id=" + id));

        inquiry.setStatus(true);
        return inquiryRepository.save(inquiry);
    }

    // 문의 삭제 (Delete)
    public void deleteInquiry(Long id) {
        inquiryRepository.deleteById(id);
    }
}