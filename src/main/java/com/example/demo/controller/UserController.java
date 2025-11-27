package com.example.demo.controller;

import com.example.demo.config.auth.PrincipalDetails;
import com.example.demo.config.auth.jwt.JWTTokenProvider;
import com.example.demo.config.auth.redis.RedisUtil;
import com.example.demo.domain.dto.*;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.entity.user.UserProfile;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.domain.service.ChatRoomService;
import com.example.demo.domain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
public class UserController {


    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    RedisUtil redisUtil;

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal PrincipalDetails principalDetails, HttpServletResponse response) throws IOException {
        log.info("GET /login..." + principalDetails);

        if(principalDetails!=null){
            response.sendRedirect("/main");
        }
        return "/login";

    }
    //확인방법 - 2
    @GetMapping("/user")
    public void user(Model model){

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        log.info("GET /user.." + authentication);
        log.info("name..." + authentication.getName());
        log.info("principal..." + authentication.getPrincipal());
        log.info("authorities..." + authentication.getAuthorities());
        log.info("details..." + authentication.getDetails());
        log.info("credential..." + authentication.getCredentials());

        model.addAttribute("auth_1",authentication);
    }

    //확인방법 - 3 Authentication's Principal 만 꺼내와 연결
    @GetMapping("/manager")
    public void manager(@AuthenticationPrincipal PrincipalDetails principalDetails){
        log.info("GET /manager.."+principalDetails);
    }
    @GetMapping("/admin")
    public void admin(){
        log.info("GET /admin..");
    }


    @GetMapping("/join")
    public String joinView() {
        log.info("GET /join...");
        return "/join";
    }

    @GetMapping("/chatPage")
    public String chatPage(Model model) {
        // 1. Spring Security Context에서 인증 객체를 가져와 PrincipalDetails 타입인지 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // email 변수를 먼저 null로 초기화
        String email = null;

        // 2. PrincipalDetails에서 이메일 추출
        if (authentication != null && authentication.getPrincipal() instanceof PrincipalDetails) {
            PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
            email = principalDetails.getUsername();
        }

        // 3. 이메일이 유효한 경우에만 DB 조회 및 모델 추가 진행
        if (email != null) {
            Optional<User> userOptional = userRepository.findByEmail(email);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                UserProfile userProfile = user.getUserProfile();
                // 4. UserProfileDto (ID, Email, NickName)를 모델에 담아 전달
                model.addAttribute("myProfile", new UserProfileDto(user.getId(), user.getEmail(), userProfile.getNickName()));

                // View Resolver에 따라 슬래시 제거 또는 유지
                return "/chatPage";
            }

            // 3-1. (DB에서 사용자를 찾을 수 없는 예외 상황)
            // 메인 페이지보다 로그인 페이지로 리다이렉션하는 것이 더 안전합니다.
            return "redirect:/login";
        }

        // 2-1. (인증 객체가 없거나 PrincipalDetails 타입이 아닌 경우)
        // 인증되지 않은 사용자이므로 로그인 페이지로 리다이렉트합니다.
        return "redirect:/login";
    }

    @GetMapping("/community")
    public String community() {
        log.info("GET /community...");
        return "/community";
    }

    @GetMapping("/cover_letter")
    public String cover_letter(){
        log.info("GET /cover_letter... ");
        return "/cover_letter";
    }

    @GetMapping("/inquiry_center")
    public String inquiry_center(){
        log.info("GET /inquiry_center... ");
        return "/inquiry_center";
    }

    @GetMapping("/inquiry_create")
    public String inquiry_create(){
        log.info("GET /inquiry_create... ");
        return "/inquiry_create";
    }

    @GetMapping("/inquiry_list")
    public String inquiry_list(){
        log.info("GET /inquiry_list... ");
        return "/inquiry_list";
    }

    /**
     * 채팅방에 추가 가능한 사용자 검색
     * 💡 String roomId를 받도록 복원
     */
    @GetMapping("/api/user/search/available")
    @Operation(summary = "추가 가능 사용자 검색",
            description = "특정 채팅방에 속하지 않은 사용자를 검색합니다.")
    public ResponseEntity<List<UserDto>> searchAvailableUsers(
            @RequestParam String query,
            @RequestParam String roomId) {

        // Service 계층 호출 (String ID 전달)
        // ChatRoomService.searchUsersNotInRoom(String query, String roomId)를 호출합니다.
        List<UserDto> users = chatRoomService.searchUsersNotInRoom(query, roomId);

        // 복원 전 코드에서 Long 변환 로직 및 예외 처리가 있었지만,
        // String ID 체계 복원을 위해 Long 변환 코드를 제거하고 String을 그대로 전달합니다.

        return ResponseEntity.ok(users);
    }


    @GetMapping("/businessChatPage")
    public String businessChatPage(){
        log.info("GET /businessChatPage... ");
        return "/businessChatPage";
    }

    @PostMapping("/join")
    public String join_post(@ModelAttribute JoinDto dto) {

        System.out.println("유저이름 : " + dto.getUserName());
        log.info("POST /join..DTO: {}", dto);

        try {
            Long userId = userService.joinRegistration(dto);
            log.info("디버그 최종: 회원가입 성공. User ID: {}", userId);
        } catch (IllegalArgumentException e) {
            // 이메일 중복 등의 검증 오류가 서비스에서 발생했을 때 처리
            log.error("회원가입 실패: {}", e.getMessage());
            // 모델에 에러 메시지를 담아 join 페이지로 리턴하거나, 쿼리 파라미터로 전달
            return "redirect:/join?error=" + e.getMessage();

        } catch (Exception e) {
            log.error("회원가입 중 서버 오류 발생", e);
            return "redirect:/join?error=서버 오류가 발생했습니다.";
        }

        // 모든 처리가 성공하면 메인으로 리다이렉트
        return "redirect:/main";
    }


    @GetMapping("/noticedetail")
    public String noticedetail() {
        log.info("GET /notice-detail...");
        return "/noticedetail";
    }
    @GetMapping("/noticepage")
    public String noticepage() {
        log.info("GET /notice-page...");
        return "/noticepage";
    }

    //내 정보
    @GetMapping("/myPage")
    public String myPage(
            // @AuthenticationPrincipal로 현재 로그인 사용자 정보(PrincipalDetails)를 받습니다.
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            Model model) {
        // 1. PrincipalDetails가 Model에 필요한지 확인하고 추가
        if (principalDetails != null) {
            model.addAttribute("userDto", principalDetails.getDto());
        }
        log.info("GET /myPage page ");
        return "myPage"; // or "/myPage"
    }

    //회원탈퇴
    @GetMapping("/quit")
    public String myAccount(){
        log.info("GET /quit page ");
        return "/quit";
    }
    @PostMapping("/quit/ajax")
    @ResponseBody
    public ResponseEntity<?> quitMemberAjax(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ModelAttribute QuitDto quitDto,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 로그인 여부 확인
        if (principalDetails == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            // 현재 사용자 이메일 추출
            String email = principalDetails.getUsername();

            // Service 계층으로 넘겨 비밀번호 검증 및 삭제 로직 실행
            userService.quitAccount(response, request, email, quitDto);

            // ✅ 성공 응답 (HTTP 200 OK)
            return ResponseEntity.ok("계정 삭제 성공"); // 클라이언트에서는 이 메시지가 아닌 HTTP 상태 코드만 확인

        } catch (IllegalArgumentException e) {
            // ❌ 비밀번호 불일치, 동의 미체크 예외 처리 (UserService.java에서 발생)
            String errorMessage = e.getMessage();

            // 요청하신 비밀번호 불일치 메시지만 팝업하도록 명확하게 설정
            if (errorMessage.contains("비밀번호가 일치하지 않습니다.")) {
                // 클라이언트 JavaScript에서 이 텍스트를 alert으로 사용합니다.
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호가 틀렸습니다 다시입력해주세요");
            }

            // 기타 IllegalArgumentException
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);

        } catch (Exception e) {
            // ⚠️ 기타 서버 오류 처리
            log.error("계정 삭제 중 서버 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("오류가 발생했습니다. 다시 진행해주세요.");
        }
    }

    //비밀번호 변경
    @ResponseBody
    @PostMapping("/quit/pwchange")
    public void ChangePassword(
            @ModelAttribute PasswordChangeDto passwordChangeDto,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    )throws Exception{

        System.out.println("DEBUG: 1. Controller ChangePassword 진입");
        String email = principalDetails.getUsername();
        System.out.println("DEBUG: 2. 로그인 사용자 이메일: " + email);

        try{
            //DB업데이트
            userService.changePassword(
                    email,
                    passwordChangeDto.getCurrentPassword(),
                    passwordChangeDto.getNewPassword()
            );
            //토큰삭제,로그아웃
            String accessToken = jwtTokenProvider.resolveToken(httpServletRequest);
            System.out.println("DEBUG: 9. 토큰 무효화 시작 (Redis 삭제)");
            if (accessToken != null) {
                Long remainingExpirationMillis = jwtTokenProvider.getRemainingExpiration(accessToken);
                //accessToken을 블랙리스트로 Redis에 등록
                if (remainingExpirationMillis > 0) {
                    redisUtil.setDataExpire(accessToken, "quit", remainingExpirationMillis);
                }
            }
            redisUtil.delete("RT : " + email);
            SecurityContextHolder.clearContext();
            System.out.println("DEBUG: 10. Security Context 초기화됨");
            httpServletResponse.sendRedirect("/login");
            System.out.println("DEBUG: 11. /login으로 리다이렉트 시도");
            return;

        }catch (IllegalArgumentException e){
            System.out.println("DEBUG: ❌ 12. IllegalArgumentException (비밀번호 불일치) 발생: " + e.getMessage());
            httpServletResponse.sendRedirect("/quit?error=" + e.getMessage());
            return;
        }catch (Exception e){
            System.out.println("DEBUG: ❌ 13. 일반 Exception (서버 오류) 발생: " + e.getMessage());
            e.printStackTrace();
            httpServletResponse.sendRedirect("/quit?error=서버 오류가 발생했습니다.");
            return;
        }
    }
}