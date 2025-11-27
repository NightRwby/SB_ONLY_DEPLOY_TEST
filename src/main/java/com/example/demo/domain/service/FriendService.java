package com.example.demo.domain.service;

import com.example.demo.domain.dto.UserDto;
import com.example.demo.domain.entity.FriendEntity;
import com.example.demo.domain.entity.FriendRequestEntity;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.entity.user.UserBlocking;
import com.example.demo.domain.repository.BlockListRepository;
import com.example.demo.domain.repository.FriendRepository;
import com.example.demo.domain.repository.FriendRequestRepository;
import com.example.demo.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FriendService {

    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final BlockListRepository blockListRepository;

    /**
     * 사용자 검색 (친구 추가용)
     * - 본인 제외
     * - 이미 친구인 사람 제외
     * - 내가 차단한 사람 제외
     * - 나를 차단한 사람 제외 (선택 사항, 일반적으로는 검색 안 되게 함)
     */
    public List<UserDto> searchUsersForFriendRequest(String query, String currentEmail) {
        User currentUser = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 키워드로 유저 검색
        List<User> users = userRepository.findByKeywordContaining(query);

        // 2. 필터링을 위한 데이터 조회
        // 내 친구 목록
        List<User> myFriends = friendRepository.findByUser(currentUser).stream()
                .map(FriendEntity::getFriend)
                .collect(Collectors.toList());

        // 내가 차단한 목록
        List<String> blockedEmails = blockListRepository.findByUser_Email(currentEmail).stream()
                .map(UserBlocking::getBlockedUserEmail)
                .collect(Collectors.toList());

        // 3. 필터링 및 DTO 변환
        return users.stream()
                .filter(user -> !user.getEmail().equals(currentEmail)) // 본인 제외
                .filter(user -> !myFriends.contains(user))             // 이미 친구인 경우 제외
                .filter(user -> !blockedEmails.contains(user.getEmail())) // 차단한 유저 제외
                // 상대방이 나를 차단했는지 확인하는 로직은 성능상 필요에 따라 추가 (여기선 생략)
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }
    /**
     * 현재 로그인된 사용자의 친구 목록 조회
     */
    public List<UserDto> getFriendsList(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return friendRepository.findByUser(user).stream()
                .map(friendEntity -> toUserDto(friendEntity.getFriend()))
                .collect(Collectors.toList());
    }

    /**
     * 사용자 검색 (이메일, 이름, 전화번호)
     */
    public List<UserDto> searchUsers(String query) {
        String currentEmail = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        return userRepository.findByKeywordContaining(query).stream()
                .filter(user -> !user.getEmail().equals(currentEmail)) // 본인 제외
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    /**
     * 친구 요청 전송
     */
    @Transactional
    public void sendFriendRequest(String senderEmail, String receiverEmail) {
        // 1. 유저 조회
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));
        User receiver = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));

        // 2. 자기 자신에게 요청 방지
        if (sender.equals(receiver)) {
            throw new IllegalArgumentException("자기 자신에게는 친구 요청을 보낼 수 없습니다.");
        }

        // 3. 이미 친구인지 확인
        if (friendRepository.existsByUserAndFriend(sender, receiver)) {
            throw new IllegalArgumentException("이미 친구입니다.");
        }

        // 4. 중복 요청 방지
        if (friendRequestRepository.existsBySenderAndReceiverAndStatus(
                sender, receiver, FriendRequestEntity.RequestStatus.PENDING)) {
            throw new IllegalArgumentException("이미 요청이 진행 중입니다.");
        }

        // 5. 요청 생성 및 저장
        FriendRequestEntity request = FriendRequestEntity.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestEntity.RequestStatus.PENDING)
                .build();

        friendRequestRepository.save(request);
        log.info("친구 요청 전송: {} -> {}", senderEmail, receiverEmail);
    }

    /**
     * 받은 친구 요청 목록 조회
     */
    public List<UserDto> getReceivedRequests(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return friendRequestRepository
                .findByReceiverAndStatus(user, FriendRequestEntity.RequestStatus.PENDING)
                .stream()
                .map(request -> toUserDto(request.getSender()))
                .collect(Collectors.toList());
    }

    /**
     * 친구 요청 수락
     */
    @Transactional
    public void acceptFriendRequest(String userEmail, String senderEmail) {
        // userEmail: 요청 받은 사람 (나, 수락하는 사람)
        // senderEmail: 요청 보낸 사람 (상대방)

        User receiver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));

        // 1. [핵심] 기존 요청 데이터 조회 (새로 만드는 게 아니라 찾아서 수정해야 함)
        FriendRequestEntity request = friendRequestRepository
                .findBySenderAndReceiverAndStatus(sender, receiver, FriendRequestEntity.RequestStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("대기 중인 친구 요청이 없습니다."));

        // 2. 상태 변경 (JPA가 감지하여 DB의 status를 ACCEPTED로 UPDATE 쿼리 날림)
        request.accept();

        // 3. 친구 관계 생성 (양방향 저장 - 조회 성능 최적화)
        // 관계 1: 나 -> 상대
        FriendEntity friendship1 = FriendEntity.builder()
                .user(receiver)
                .friend(sender)
                .build();

        // 관계 2: 상대 -> 나
        FriendEntity friendship2 = FriendEntity.builder()
                .user(sender)
                .friend(receiver)
                .build();

        // 두 개의 관계를 저장
        friendRepository.save(friendship1);
        friendRepository.save(friendship2);

        log.info("친구 요청 수락 완료: {} <-> {}", userEmail, senderEmail);
    }

    /**
     * 친구 요청 거절
     */
    @Transactional
    public void rejectFriendRequest(String userEmail, String senderEmail) {
        User receiver = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new IllegalArgumentException("발신자를 찾을 수 없습니다."));

        FriendRequestEntity request = friendRequestRepository
                .findBySenderAndReceiverAndStatus(
                        sender, receiver, FriendRequestEntity.RequestStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 요청입니다."));

        request.reject();
        log.info("친구 요청 거절: {} -> {}", senderEmail, userEmail);
    }

    /**
     * 친구 삭제
     */
    @Transactional
    public void deleteFriend(String userEmail, String friendEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        User friend = userRepository.findByEmail(friendEmail)
                .orElseThrow(() -> new IllegalArgumentException("친구를 찾을 수 없습니다."));

        // 양방향 관계 삭제
        friendRepository.findByUserAndFriend(user, friend).ifPresent(friendRepository::delete);
        friendRepository.findByUserAndFriend(friend, user).ifPresent(friendRepository::delete);

    }

    /**
     * 유저 차단 (친구 관계 삭제 포함)
     */
    @Transactional
    public void blockUserAndRemoveFriend(String blockerEmail, String blockedEmail) {
        User blocker = userRepository.findByEmail(blockerEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        User blocked = userRepository.findByEmail(blockedEmail)
                .orElseThrow(() -> new IllegalArgumentException("차단할 대상을 찾을 수 없습니다."));

        // 1. 친구 관계 삭제 (양방향)
        friendRepository.findByUserAndFriend(blocker, blocked).ifPresent(friendRepository::delete);
        friendRepository.findByUserAndFriend(blocked, blocker).ifPresent(friendRepository::delete);
        // 3. 차단 목록에 추가 (UserBlocking)
        if (!blockListRepository.existsByUser_EmailAndBlockedUserEmail(blockerEmail, blockedEmail)) {
            UserBlocking blocking = UserBlocking.builder()
                    .user(blocker)
                    .blockedUserEmail(blockedEmail)
                    .blockedAt(LocalDateTime.now())
                    .build();
            blockListRepository.save(blocking);
        }
    }
    // User -> UserDto 변환
    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .email(user.getEmail())
                .userName(user.getUserName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .build();
    }
}