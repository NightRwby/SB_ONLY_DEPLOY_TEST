package com.example.demo.controller;

import com.example.demo.domain.dto.ChatRoomDto;
import com.example.demo.domain.service.ChatRoomService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
@Slf4j
public class RoomController {

    private final ChatRoomService chatRoomService;

    // HTML 뷰 반환: 채팅방 목록 페이지 (URL: /chat/room)
    @GetMapping("/room")
    public String rooms(Model model, HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return "redirect:/login"; // 로그인 체크
        }
        model.addAttribute("userEmail", userEmail);
        return "chat/room"; // chat/room.html
    }

    // HTML 뷰 반환: 채팅방 입장 (URL: /chat/room/enter/{roomId})
    @GetMapping("/room/enter/{roomId}")
    public String roomDetail(Model model, @PathVariable("roomId") String roomId, HttpSession session) {
        String userEmail = (String) session.getAttribute("userEmail");
        if (userEmail == null) {
            return "redirect:/login";
        }

        ChatRoomDto room = chatRoomService.findRoomById(roomId);
        if (room == null) {
            return "redirect:/chat/room";
        }

        model.addAttribute("room", room);
        model.addAttribute("sender", userEmail);
        return "chat/roomdetail"; // chat/roomdetail.html
    }
}