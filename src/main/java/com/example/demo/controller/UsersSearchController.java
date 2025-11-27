package com.example.demo.controller;



import com.example.demo.domain.dto.UserDto;
import com.example.demo.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UsersSearchController {

    // UserService를 주입받아 검색 로직을 수행합니다.
    private final UserService userService;

    @GetMapping("/api/user/search")
    public List<UserDto> searchUsers(@RequestParam("query") String query) {
        // 현재 로그인한 사용자 이메일 가져오기
        String currentEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 수정된 서비스 메서드 호출
        return userService.searchUsersForBlock(query, currentEmail);
    }
}