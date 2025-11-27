package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@RequestMapping("/api/chat") // HTTP 요청의 기본 경로 설정: /api/chat
public class ChatPageController {

    // 포트폴리오 보관함
    @GetMapping("/portfolio")
    public String showPortfolio() {
        return "/portfolio"; // portfolio.html
    }

    // 자기소개서 보관함
    @GetMapping("/coverLetter")
    public String showCoverLetter() {
        return "/cover_letter"; //cover_letter.html
    }

    // 공지사항
    @GetMapping("/noticepage")
    public String showNoticePage(){
        return "/noticepage";
    }

    // 자유게시판
    @GetMapping("/free_community")
    public String showFreeCommunity(){ return "free_community";};

    // 핫이슈 페이지
    @GetMapping("/hotissuePage")
    public String showHotIssue(){
        return "/hotissuePage";
    }

    //문서 페이지
    @GetMapping("/document")
    public String showDocument(){
        return "/document";
    }

}
