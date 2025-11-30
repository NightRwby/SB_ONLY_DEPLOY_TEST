package com.example.demo.domain.service;

import com.example.demo.config.auth.jwt.JWTTokenProvider;
import com.example.demo.config.auth.logoutHandler.CustomLogoutSuccessHandler;
import com.example.demo.config.auth.redis.RedisUtil;
import com.example.demo.domain.dto.*;
import com.example.demo.domain.dto.inputDto.BlockRequest;
import com.example.demo.domain.dto.outputDto.BlockResponse;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.entity.user.UserBlocking;
import com.example.demo.domain.entity.user.UserProfile;
import com.example.demo.domain.repository.BlockListRepository;
import com.example.demo.domain.repository.UserProfileRepository;
import com.example.demo.domain.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private JWTTokenProvider jwtTokenProvider;

    @Autowired
    private CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @Autowired
    private BlockListRepository blockListRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private FileService fileService;

    // 회원가입
    @Transactional
    public Long joinRegistration(JoinDto dto) throws Exception {
        log.info("디버그 1: joinRegistration 시작. DTO 이메일: {}", dto.getEmail());

        // 1. User 엔티티 생성 및 저장
        String encodedPassword = encoder.encode(dto.getPassWord());
        User user = User.builder()
                .email(dto.getEmail())
                .userName(dto.getUserName())
                .passWord(encodedPassword)
                .phoneNumber(null)
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        log.info("디버그 2: User 저장 완료. 할당된 User ID: {}, 이메일: {}", user.getId(), user.getEmail());

        // 2. UserProfile 엔티티 생성 및 필수 필드 초기화
        UserProfile userProfile = UserProfile.builder()
                .nickname(user.getUserName())
                .profileImageUrl(null)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isEnabled(true)
                .isCredentialsNonExpired(true)
                .readReceiptsEnabled(false)
                .stateMessage("")
                .businessEmail(null)
                .build();

        log.info("디버그 3: UserProfile 객체 생성 완료. 초기 닉네임: {}", userProfile.getNickname());

        // 3. 관계 설정 (양방향 연결)
        userProfile.setUser(user);
        log.info("디버그 4: UserProfile에 User 관계 설정 완료. UserProfile 참조 이메일: {}", userProfile.getUser().getEmail());

        user.updateUserProfile(userProfile);

        // 4. UserProfile 엔티티 저장
        log.info("디버그 5: UserProfile 저장 시도.");
        userProfileRepository.save(userProfile);

        log.info("디버그 6: UserProfile 저장 호출 완료. (트랜잭션 커밋 대기 중)");

        return user.getId();
    }

    // 프로필 업데이트 (사진 포함)
    @Transactional
    public void updateProfile(String email, ProfileUpdateDto dto) throws IOException {
        // 1. 엔티티 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
        UserProfile userProfile = user.getUserProfile();

        // 파일 업데이트
        MultipartFile file = dto.getProfileImageUrl();
        log.info("CHECK_FILE_STATE: MultipartFile file is null? {}", (file == null));
        log.info("CHECK_FILE_STATE: MultipartFile file size: {}", (file != null ? file.getSize() : "N/A"));

        if (file != null && !file.isEmpty()) {
            String storageKey = fileService.uploadAndReturnStorageKey(file, "profile");
            log.info("CHECK_FILE_STATE: IF BLOCK ENTERED. Starting file upload...");
            log.info("DEBUG: Profile File received: {}", file != null && !file.isEmpty());
            log.info("DEBUG: Profile Storage Key: {}", storageKey);
            String profileDownloadUrl = "/api/profile/download/" + storageKey;
            userProfile.setProfileImageUrl(profileDownloadUrl);
        }

        MultipartFile bannerFile = dto.getBannerImageUrl();
        if (bannerFile != null && !bannerFile.isEmpty()) {
            String storageKey = fileService.uploadAndReturnStorageKey(bannerFile, "banner");
            String bannerDownloadUrl = "/api/profile/download/" + storageKey;
            userProfile.setBannerImageUrl(bannerDownloadUrl);
        } else {
            log.info("INFO: Banner file not provided, retaining existing URL.");
        }

        // 기타 필드 업데이트
        if (dto.getNickname() != null) {
            userProfile.setNickname(dto.getNickname());
        }
        if (dto.getStateMessage() != null) {
            userProfile.setStateMessage(dto.getStateMessage());
        }
        log.warn("FINAL_PROFILE_URL: {}", userProfile.getProfileImageUrl());
        userRepository.flush();
    }

    @Transactional
    public void deleteBannerImage(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        UserProfile userProfile = user.getUserProfile();

        userProfile.setBannerImageUrl(null);
    }

    public List<UserDto> searchUsersByKeyword(String keyword) {
        String currentLoginEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        List<User> foundUsers = userRepository.findByKeywordContaining(keyword);

        return foundUsers.stream()
                .filter(user -> !user.getEmail().equals(currentLoginEmail))
                .map(user -> UserDto.builder()
                        .email(user.getEmail())
                        .userName(user.getUsername())
                        .build())
                .collect(Collectors.toList());
    }

    // 회원탈퇴
    @Transactional
    public void quitAccount(HttpServletResponse response, HttpServletRequest request, String email, QuitDto quitDto) throws ServletException, IOException {
        System.out.println(">>> quitAccount() 진입 성공");

        String password = quitDto.getPassword();
        boolean isAgreed = quitDto.isAgreed();
        System.out.println("탈퇴 확인 패스워드 : " + password);
        System.out.println("탈퇴 확인 동의 : " + isAgreed);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        System.out.println("사용자 email : " + user.getEmail());
        log.info("DB 비밀번호: {}", user.getPassword());
        log.info("matches 결과: {}", encoder.matches(password, user.getPassword()));

        if (!quitDto.isAgreed()) throw new IllegalArgumentException("항목에 동의해야 회원탈퇴가 됩니다.");

        if (!encoder.matches(password, user.getPassword()))
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");

        String accessToken = jwtTokenProvider.resolveToken(request);
        if (accessToken != null) {
            Long remainingExpirationMillis = jwtTokenProvider.getRemainingExpiration(accessToken);
            if (remainingExpirationMillis > 0) {
                redisUtil.setDataExpire(accessToken, "quit", remainingExpirationMillis);
            }
        }
        redisUtil.delete("RT : " + user.getEmail());
        System.out.println("DEBUG: User Delete Attempt for 레디스 토큰삭제 확인 : " + user.getEmail());

        userRepository.deleteByEmail(email);
        System.out.println("DB삭제 완료");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        customLogoutSuccessHandler.onLogoutSuccess(request, response, authentication);
        SecurityContextHolder.clearContext();
    }

    // 비밀번호 변경
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        System.out.println("DEBUG: 6. DB에서 사용자 로드됨: " + user.getEmail());

        if (!encoder.matches(currentPassword, user.getPassword()))
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        System.out.println("DEBUG: 7. PW 비교 시도 (입력된 PW와 DB PW): " + currentPassword);

        String encodedNewPassword = encoder.encode(newPassword);
        user.updatePassword(encodedNewPassword);
        System.out.println("비밀번호 업데이트 완료");
    }

    // 회원 차단
    @Transactional
    public void blockUser(String email, BlockRequest blockRequest) {
        String blockedEmail = blockRequest.getTargetUserEmail();
        User blockerUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("차단사용자를 찾을 수 없습니다." + email));

        if (blockListRepository.existsByUser_EmailAndBlockedUserEmail(email, blockedEmail)) {
            throw new IllegalArgumentException("이미 차단한 사용자입니다.");
        }

        UserBlocking newBlock = UserBlocking.builder()
                .user(blockerUser)
                .blockedUserEmail(blockedEmail)
                .blockedAt(LocalDateTime.now())
                .build();
        blockListRepository.save(newBlock);
    }

    @Transactional(readOnly = true)
    public List<BlockResponse> getBlockedUsers(String email) {
        // 내가(User) 차단한 목록을 조회하도록 변경 (findByUser_Email)
        List<UserBlocking> blockings = blockListRepository.findByUser_Email(email);

        return blockings.stream()
                .map(blocking -> {
                    return new BlockResponse(
                            blocking.getBlockedUserEmail(),
                            blocking.getBlockedAt(),
                            null
                    );
                })
                .collect(Collectors.toList());
    }

    // 차단 검색 (전체 유저 중 키워드로 찾기)
    public List<UserSearchDto> searchUsersByQuery(String query) {
        List<User> users = userRepository.findByUserNameContainingOrEmailContaining(query, query);
        return users.stream()
                .map(UserSearchDto::new)
                .collect(Collectors.toList());
    }

    // 차단 해제
    @Transactional
    public void unblockUser(String userEmail, BlockRequest blockRequest) {
        String targetEmail = blockRequest.getTargetUserEmail();
        UserBlocking blockEntry = blockListRepository.findByUser_EmailAndBlockedUserEmail(userEmail, targetEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자에 대한 차단 기록이 존재하지 않습니다."));
        blockListRepository.delete(blockEntry);
    }

    /**
     * 사용자 검색 (차단 대상 찾기용)
     * - 나 자신 제외
     * - 이미 차단한 사용자 제외
     */
    public List<UserDto> searchUsersForBlock(String keyword, String currentLoginEmail) {
        List<User> allFoundUsers = userRepository.findByUserNameContainingOrEmailContaining(keyword, keyword);
        List<UserBlocking> myBlockList = blockListRepository.findByUser_Email(currentLoginEmail);
        Set<String> blockedEmails = myBlockList.stream()
                .map(UserBlocking::getBlockedUserEmail)
                .collect(Collectors.toSet());

        return allFoundUsers.stream()
                .filter(user -> !user.getEmail().equals(currentLoginEmail)) // 나 제외
                .filter(user -> !blockedEmails.contains(user.getEmail()))   // 이미 차단한 사람 제외
                .map(user -> UserDto.builder()
                        .email(user.getEmail())
                        .userName(user.getUserName())
                        .build())
                .collect(Collectors.toList());
    }

    // 프로필 정보 조회
    @Transactional(readOnly = true)
    public ProfileResponseDto getProfileData(String email) {
        if (email == null || email.trim().isEmpty()) {
            return new ProfileResponseDto();
        }
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isEmpty()) {
            return new ProfileResponseDto();
        }
        User user = userOptional.get();
        UserProfile profile = user.getUserProfile();

        return new ProfileResponseDto(
                profile.getNickname(),
                profile.getStateMessage(),
                profile.getProfileImageUrl(),
                profile.getBannerImageUrl()
        );
    }
}