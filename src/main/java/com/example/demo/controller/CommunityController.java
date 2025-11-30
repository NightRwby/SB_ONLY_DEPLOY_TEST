package com.example.demo.controller;

import com.example.demo.config.auth.PrincipalDetails;
import com.example.demo.domain.dto.UserProfileDto; // [추가] DTO import
import com.example.demo.domain.entity.CommunityEntity;
import com.example.demo.domain.entity.CommunityImageEntity;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.entity.user.UserProfile;
import com.example.demo.domain.repository.UserRepository; // [추가] Repo import
import com.example.demo.domain.service.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [추가] 로그용
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // [추가]
import org.springframework.security.core.userdetails.UserDetails; // [추가]
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/community")
@RequiredArgsConstructor
@Slf4j
public class CommunityController {

    private final CommunityService communityService;
    private final UserRepository userRepository;

    private void addProfileToModel(Model model, PrincipalDetails principalDetails) {
//        log.info("[디버그].."+principalDetails);
        if (principalDetails != null) {
            String email = principalDetails.getEmail();
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                UserProfile profile = user.getUserProfile();
                model.addAttribute("myProfile", new UserProfileDto(user.getId(), user.getEmail(), user.getNickName(), profile.getProfileImageUrl(), profile.getStateMessage(), profile.getOnlineStatus()));

            }
        }
    }

    @GetMapping("/home")
    public String home(Model model, @AuthenticationPrincipal PrincipalDetails principalDetails) { // [수정] 인증 객체 받기
        addProfileToModel(model, principalDetails); // 프로필 정보 주입
        model.addAttribute("category", "home");
        return "board/community";
    }

    @GetMapping("/free/list")
    public String freeList(Model model,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String sort,
                           @RequestParam(defaultValue = "1") int page,
                           @AuthenticationPrincipal PrincipalDetails principalDetails) {

        addProfileToModel(model, principalDetails);

        Page<CommunityEntity> boardList = communityService.getBoardList("free", keyword, category, sort, page);

        int blockLimit = 5;
        int startPage = (((int) (Math.ceil((double) page / blockLimit))) - 1) * blockLimit + 1;
        int endPage = Math.min((startPage + blockLimit - 1), boardList.getTotalPages());
        if (endPage == 0) endPage = 1;

        model.addAttribute("boardList", boardList);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("category", category);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);

        return "board/free_Community";
    }

    // 글쓰기 (파일 업로드 포함)
    @PostMapping("/free/write")
    public String freeWrite(@RequestParam String title,
                            @RequestParam String content,
                            @RequestParam String category,
                            @RequestParam(required = false) List<MultipartFile> files,
                            @AuthenticationPrincipal PrincipalDetails principalDetails ) throws IOException {
        String writer = "익명"; //기본값
        if (principalDetails != null){
            String email = principalDetails.getEmail();
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()){
                writer = userOptional.get().getNickName();
            }
        }

        communityService.writeBoard(title, content, "free", category, writer, files);
        return "redirect:/community/free/list";
    }

    // [수정됨] 글 수정 (clearExistingFiles 플래그 추가)
    @PostMapping("/free/update")
    public String freeUpdate(@RequestParam Long id,
                             @RequestParam String title,
                             @RequestParam String content,
                             @RequestParam String category,
                             @RequestParam(required = false) List<MultipartFile> files,
                             @RequestParam(defaultValue = "false") boolean clearExistingFiles) throws IOException { // ADDED

        // =========================================================
        // ★★★ [디버그 로그] 이 값이 true로 와야 삭제가 실행됩니다 ★★★
        System.out.println("--- DEBUG: Image Clear Flag Received ---");
        System.out.println("Received clearExistingFiles value: " + clearExistingFiles);
        System.out.println("----------------------------------------");
        // =========================================================


        // Service에 files와 clearExistingFiles 플래그 전달
        communityService.updateBoard(id, title, content, category, files, clearExistingFiles);
        return "redirect:/community/free/list";
    }

    // 상세보기 API
    @GetMapping("/api/detail/{id}")
    @ResponseBody
    public CommunityEntity getDetailApi(@PathVariable Long id) {
        return communityService.getBoardDetail(id);
    }

    // 삭제 API
    @DeleteMapping("/api/delete/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteApi(@PathVariable Long id) {
        communityService.deleteBoard(id);
        return ResponseEntity.ok("deleted");
    }

    // 추천 API
    @PostMapping("/api/like/{id}")
    @ResponseBody
    public ResponseEntity<Integer> likeApi(@PathVariable Long id, @RequestParam boolean status) {
        int newLikes = communityService.updateLikes(id, status);
        return ResponseEntity.ok(newLikes);
    }

    // DB 이미지 불러오기
    @GetMapping("/image/view/{id}")
    public ResponseEntity<byte[]> viewImage(@PathVariable Long id) {
        CommunityImageEntity image = communityService.getImage(id);

        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(image.getImageData());
    }

    // 기타 게시판 매핑
    @GetMapping("/noticepage")
    public String noticeList(Model model) {
        model.addAttribute("category", "notice");
        return "noticepage";
    }

    @GetMapping("/cover-letter/list")
    public String coverLetterList(Model model) {
        model.addAttribute("category", "cover-letter");
        return "board/cover_letter";
    }

    @GetMapping("/hot/list")
    public String hotList(Model model) {
        model.addAttribute("category", "hot");
        return "board/hotIssue";
    }
}