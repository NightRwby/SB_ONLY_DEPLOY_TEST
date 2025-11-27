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

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
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

    //회원가입
    @Transactional
    public Long joinRegistration(JoinDto dto) throws Exception {
        log.info("디버그 1: joinRegistration 시작. DTO 이메일: {}", dto.getEmail());

        // 1. User 엔티티 생성 및 저장
        String encodedPassword = encoder.encode(dto.getPassWord());
        User user = User.builder()
                .email(dto.getEmail())
                .userName(dto.getUserName())
                .passWord(encodedPassword)
                .phoneNumber(dto.getPhoneNumber())
                .role("ROLE_USER")
                .build();

        userRepository.save(user);
        // User 저장 후 ID가 할당되었는지 확인
        log.info("디버그 2: User 저장 완료. 할당된 User ID: {}, 이메일: {}", user.getId(), user.getEmail());


        // 2. UserProfile 엔티티 생성 및 필수 필드 초기화
        UserProfile userProfile = UserProfile.builder()
                .nickName(user.getUserName())
                .profileImageUrl("/default/image.png")

                // NOT NULL 제약조건 필드 초기화 (DB 오류 방지)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isEnabled(true)
                .isCredentialsNonExpired(true)
                .readReceiptsEnabled(false)
                .bio("")
                .businessEmail(null)
                .build();

        // UserProfile 객체의 초기 데이터 확인
        log.info("디버그 3: UserProfile 객체 생성 완료. 초기 닉네임: {}", userProfile.getNickName());

        // 3. 관계 설정 (양방향 연결)

        // 3-1. 관계의 주인 (UserProfile) 쪽에 User 연결 (FK 값 설정)
        userProfile.setUser(user);
        // FK 참조 대상이 제대로 연결되었는지 확인
        log.info("디버그 4: UserProfile에 User 관계 설정 완료. UserProfile 참조 이메일: {}", userProfile.getUser().getEmail());

        // 3-2. 관계의 비주인 (User) 쪽에 UserProfile 연결 (편의를 위한 양방향)
        user.updateUserProfile(userProfile);

        // 4. UserProfile 엔티티 저장
        log.info("디버그 5: UserProfile 저장 시도.");
        userProfileRepository.save(userProfile);

        // 저장이 성공적으로 호출되었는지 확인
        log.info("디버그 6: UserProfile 저장 호출 완료. (트랜잭션 커밋 대기 중)");

        return user.getId();
    }

    //프로필사진 업데이트
    @Transactional
    public void updateProfile(String email, ProfileUpdateDto dto) throws IOException {
        var UpdateImg = userRepository.findByEmail(email).orElseThrow();
    }

    public UserService(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }


    public List<UserDto> searchUsersByKeyword(String keyword) {
        // 현재 로그인된 사용자의 이메일 획득 (검색 결과에서 제외하기 위함)
        String currentLoginEmail = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. Repository를 사용하여 User 엔티티 검색
        List<User> foundUsers = userRepository.findByKeywordContaining(keyword);

        // 2. 검색된 User 엔티티를 필터링 및 List<UserDto>로 변환하여 반환
        return foundUsers.stream()
                // 현재 로그인된 사용자 제외
                .filter(user -> !user.getEmail().equals(currentLoginEmail))
                // User 엔티티를 UserDto로 변환 (UserDto 빌더가 필요합니다. Long id 필드는 제외해도 무방)
                .map(user -> UserDto.builder()
                        .email(user.getEmail())
                        .userName(user.getUsername())
                        .build())
                .collect(Collectors.toList());
    }

    //회원탈퇴
    @Transactional
    public void quitAccount(HttpServletResponse response, HttpServletRequest request, String email, QuitDto quitDto) throws ServletException, IOException {
        System.out.println(">>> quitAccount() 진입 성공");

        //form에서 받은 정보
        String password = quitDto.getPassword();
        boolean isAgreed = quitDto.isAgreed();
        System.out.println("탈퇴 확인 패스워드 : " + password);
        System.out.println("탈퇴 확인 동의 : " + isAgreed);


        //엔티티에서 사용자 찾기
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        System.out.println("사용자 email : " + user.getEmail());
        log.info("DB 비밀번호: {}", user.getPassword());
        log.info("matches 결과: {}", encoder.matches(password, user.getPassword()));

        //탈퇴 확인
        if (!quitDto.isAgreed()) throw new IllegalArgumentException("항목에 동의해야 회원탈퇴가 됩니다.");

        if (!encoder.matches(password, user.getPassword()))
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");

        //accessToken추출 , 블랙리스트 등록
        String accessToken = jwtTokenProvider.resolveToken(request);
        if (accessToken != null) {
            Long remainingExpirationMillis = jwtTokenProvider.getRemainingExpiration(accessToken);
            //accessToken을 블랙리스트로 Redis에 등록
            if (remainingExpirationMillis > 0) {
                redisUtil.setDataExpire(accessToken, "quit", remainingExpirationMillis);
            }
        }
        redisUtil.delete("RT : " + user.getEmail());
        System.out.println("DEBUG: User Delete Attempt for 레디스 토큰삭제 확인 : " + user.getEmail());
        //DB삭제
        userRepository.deleteByEmail(email);
        System.out.println("DB삭제 완료");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        customLogoutSuccessHandler.onLogoutSuccess(request, response, authentication);
        SecurityContextHolder.clearContext();
    }

    //비밀번호 변경
    @Transactional
    public void changePassword(String email,String currentPassword, String newPassword) {
        //사용자 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        System.out.println("DEBUG: 6. DB에서 사용자 로드됨: " + user.getEmail());

        //기존 비밀번호 일치 여부 확인
        if (!encoder.matches(currentPassword, user.getPassword()))
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        System.out.println("DEBUG: 7. PW 비교 시도 (입력된 PW와 DB PW): " + currentPassword);

        //비밀번호 암호화 , 업데이트
        String encodedNewPassword = encoder.encode(newPassword);
        user.updatePassword(encodedNewPassword);
        System.out.println("비밀번호 업데이트 완료");
    }



    //회원-회원차단
    @Transactional
    public void blockUser(String email, BlockRequest blockRequest){
        //DTO에서 차단할 상대방 이메일 가져오기
        String blockedEmail = blockRequest.getTargetUserEmail();
        //차단하는사람 UserEntity조회
        User blockerUser = userRepository.findByEmail(email)
                .orElseThrow(()->new RuntimeException("차단사용자를 찾을 수 없습니다."+email));
        //이미 차단했는지 확인
        if(blockListRepository.existsByUser_EmailAndBlockedUserEmail(email,blockedEmail)){
            throw new IllegalArgumentException("이미 차단한 사용자입니다.");
        }
        //Entity생성 및 데이터 삽입
        UserBlocking newBlock = UserBlocking.builder()
                .user(blockerUser)
                .blockedUserEmail(blockedEmail)
                .blockedAt(LocalDateTime.now())
                .build();
        blockListRepository.save(newBlock);
    }

    @Transactional(readOnly = true)
    public List<BlockResponse> getBlockedUsers(String email){

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

    //차단검색
    public List<UserSearchDto> searchUsersByQuery(String query){

        List<User> users = userRepository.findByUserNameContainingOrEmailContaining(query,query);

        return users.stream()
                .map(UserSearchDto::new)
                .collect(Collectors.toList());
    }

    //차단해제
    @Transactional
    public void unblockUser(String userEmail,BlockRequest blockRequest){
        //차단기록조회
        String targetEmail = blockRequest.getTargetUserEmail();
        UserBlocking blockEntry = blockListRepository.findByUserEmailAndBlockedUserEmail(userEmail,targetEmail)
                .orElseThrow(()->new IllegalArgumentException("해당 사용자에 대한 차단 기록이 존재하지 않습니다."));
        //차단 기록 db삭제
        blockListRepository.delete(blockEntry);
    }

    /**
     * 사용자 검색 (차단 대상 찾기용)
     * - 나 자신 제외
     * - 이미 차단한 사용자 제외
     */
    public List<UserDto> searchUsersForBlock(String keyword, String currentLoginEmail) {

        // 1. 키워드로 모든 사용자 1차 검색 (DB)
        // (UserRepository에 findByUserNameContainingOrEmailContaining 메서드가 있다고 가정)
        List<User> allFoundUsers = userRepository.findByUserNameContainingOrEmailContaining(keyword, keyword);

        // 2. 내가 이미 차단한 사람들의 이메일 목록 조회
        List<UserBlocking> myBlockList = blockListRepository.findByUser_Email(currentLoginEmail);
        Set<String> blockedEmails = myBlockList.stream()
                .map(UserBlocking::getBlockedUserEmail)
                .collect(Collectors.toSet());

        // 3. 필터링: (나 자신 제외) AND (이미 차단한 사람 제외)
        return allFoundUsers.stream()
                .filter(user -> !user.getEmail().equals(currentLoginEmail)) // 나 제외
                .filter(user -> !blockedEmails.contains(user.getEmail()))   // 이미 차단한 사람 제외
                .map(user -> UserDto.builder()
                        .email(user.getEmail())
                        .userName(user.getUsername())
                        // 필요한 경우 프로필 이미지 경로도 DTO에 추가하면 좋습니다.
                        .build())
                .collect(Collectors.toList());
    }


}