package com.example.demo.domain.service;

import com.example.demo.domain.dto.*;
import com.example.demo.domain.entity.ChatMessageEntity;
import com.example.demo.domain.entity.ChatRoomEntity;
import com.example.demo.domain.entity.RoomMemberEntity;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.enums.RoomType;
import com.example.demo.domain.repository.ChatMessageRepository;
import com.example.demo.domain.repository.ChatRoomRepository;
import com.example.demo.domain.repository.RoomMemberRepository;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.util.KoreanNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
// 🔑 추가: 권한 관련 예외 처리를 위한 import
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageService chatMessageService;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    // ChatRoomEntity -> ChatRoomDto DTO 변환 메서드 (기본 정보)
    private ChatRoomDto toDto(ChatRoomEntity entity) {
        // [원래 코드 유지] entity.getUserCount()가 0일 경우 0으로 처리
        Integer userCount = entity.getUserCount() != 0 ? entity.getUserCount() : 0;
        return ChatRoomDto.builder()
                .roomId(entity.getRoomId())
                .roomName(entity.getRoomName())
                .type(entity.getType().name())
                .memberCount(userCount)
                .build();
    }

    // 유저가 방을 목격한 마지막시간 포함한 Dto (안 읽은 카운트 및 메시지 정보 포함)
    private ChatRoomDto toDtoWithUnreadCount(RoomMemberEntity roomMember, long unreadCount) {
        ChatRoomEntity entity = roomMember.getRoom();
        // [원래 코드 유지] entity.getUserCount()가 0일 경우 0으로 처리
        Integer userCount = entity.getUserCount() != 0 ? entity.getUserCount() : 0;

        // 1. 마지막 메시지 조회 (최적화 쿼리 사용)
        Optional<ChatMessageEntity> lastMessageOpt = chatMessageRepository
                .findTopByRoomIdOrderByCreatedAtDesc(entity.getRoomId());

        String lastMessage = lastMessageOpt.map(ChatMessageEntity::getMessage).orElse("새로운 채팅방입니다.");

        // RoomMemberEntity(roomMember)에서 joinedAt을 가져옵니다.
        LocalDateTime lastMessageTime = lastMessageOpt.map(ChatMessageEntity::getCreatedAt).orElse(roomMember.getJoinedAt());

        return ChatRoomDto.builder()
                .roomId(entity.getRoomId())
                .roomName(entity.getRoomName())
                .type(entity.getType().name())
                .memberCount(userCount)
                .unreadCount(unreadCount)
                .lastMessage(lastMessage)
                .lastMessageTime(lastMessageTime)
                .build();
    }

    /**
     * 모든 채팅방 목록 조회 (DTO 리스트 반환)
     */
    public List<ChatRoomDto> findAllRoom() {
        return chatRoomRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * ID로 채팅방 단건 조회
     */
    public ChatRoomDto findRoomById(String roomId) {
        return chatRoomRepository.findByRoomId(roomId)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * 새로운 채팅방 생성 및 DB 저장 (트랜잭션 필요)
     */
    @Transactional
    public ChatRoomDto createRoom(ChatRoomRequestDto requestDto) {

        // DTO에서 필요한 데이터 추출
        String name = requestDto.getRoomName();
        String roomOwnerId = requestDto.getCreatorId();
        RoomType type = RoomType.valueOf(requestDto.getRoomType());
        List<String> requestedMemberIds = requestDto.getMemberIds(); // 클라이언트가 보낸 리스트

        // 1. 방 생성자 (Owner) 정보 조회
        User roomOwner = userRepository.findByEmail(roomOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("방 생성자 유저를 찾을 수 없습니다."));

        // 그룹 채팅 시 방 이름이 DTO에 없으면 생성자 이름으로 초기 설정
        if (name == null || name.trim().isEmpty()) {
            name = roomOwner.getUserName() + "의 채팅방";
        }

        // 2. 초기 멤버 목록에 생성자를 강제 포함하고 중복 제거 (수정된 로직)
        Set<String> uniqueMemberEmails = new HashSet<>(requestedMemberIds);
        uniqueMemberEmails.add(roomOwnerId);

        // 3. ChatRoomEntity 생성 및 DB 저장
        String uuid = UUID.randomUUID().toString();
        ChatRoomEntity entity = ChatRoomEntity.builder()
                .roomId(uuid)
                .roomName(name) // 보강된 name 사용
                .roomOwner(roomOwnerId) // Owner ID (Email) 저장: 생성자가 누구인지 기록
                .type(type)
                .build();

        ChatRoomEntity savedEntity = chatRoomRepository.save(entity);

        // 4. RoomMemberEntity 생성 및 저장
        // Set을 List로 변환하여 Repository에 전달 (findByEmailIn이 List를 요구할 경우 대비)
        List<String> memberEmailList = new ArrayList<>(uniqueMemberEmails);

        List<User> usersToAdd = userRepository.findByEmailIn(memberEmailList);

        // User 조회가 실패했는지 확인 (RoomMember 누락 방지)
        if (usersToAdd.isEmpty()) {
            log.warn("[WARN] 채팅방 생성 중 RoomMember 저장 실패: 유효한 멤버 이메일이 없습니다. DTO: {}", requestDto);
            // 유효한 유저가 없어도 방 자체는 생성되었으므로 DTO 반환
            return toDto(savedEntity);
        }

        List<RoomMemberEntity> roomMembers = usersToAdd.stream()
                .map(user -> RoomMemberEntity.builder()
                        .room(savedEntity)
                        .user(user)
                        .build())
                .collect(Collectors.toList());

        log.info("Saving {} RoomMember entities for room {}.", roomMembers.size(), uuid);
        roomMemberRepository.saveAll(roomMembers);

        // 5. DTO로 변환하여 반환
        return toDto(savedEntity);
    }

    public boolean isRoomOwner(String roomId, String userEmail) {
        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // ChatRoomEntity에 저장된 roomOwnerId와 현재 유저의 이메일을 비교
        return room.getRoomOwner().equals(userEmail);
    }

    @Transactional
    public void addMembers(UUID roomId, List<String> memberIdentifiers) {
        String roomUuid = roomId.toString();

        // 1. 채팅방과 현재 유저 확인
        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomUuid)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. (ID: " + roomUuid + ")"));

        String inviterEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User inviter = userRepository.findByEmail(inviterEmail)
                .orElseThrow(() -> new IllegalStateException("초대하는 유저 정보를 찾을 수 없습니다."));

        // 2. 추가할 유저 조회 및 중복 제거
        List<User> usersToAdd = userRepository.findByEmailIn(memberIdentifiers);

        List<RoomMemberEntity> newRoomMembers = usersToAdd.stream()
                .filter(user -> roomMemberRepository.findByRoomAndUser(room, user).isEmpty())
                .map(user -> RoomMemberEntity.builder()
                        .room(room)
                        .user(user)
                        .build())
                .collect(Collectors.toList());

        if (newRoomMembers.isEmpty()) {
            log.info("추가할 신규 멤버가 없습니다. (room: {})", roomUuid);
            return;
        }

        // 3. DB 저장 및 엔티티 업데이트
        roomMemberRepository.saveAll(newRoomMembers);

        // 4. 시스템 메시지 생성 및 전송 (WebSocket)
        String inviterName = inviter.getUserName();
        List<String> addedNames = newRoomMembers.stream().map(m -> m.getUser().getUserName()).collect(Collectors.toList());
        String representativeName = addedNames.get(0);

        String systemMessageText;
        if (addedNames.size() == 1) {
            systemMessageText = inviterName + "님이 " + representativeName + "님을 초대했습니다.";
        } else {
            // 첫 번째 이름만 대표로 표시
            systemMessageText = inviterName + "님이 " + representativeName + "님 외 " + (addedNames.size() - 1) + "명을 초대했습니다.";
        }

        // 시스템 메시지 DTO에 senderName 및 senderInitial 필드 추가
        ChatMessageDto systemMessage = ChatMessageDto.builder()
                .type(ChatMessageDto.MessageType.ENTER)
                .chatType(ChatMessageDto.ChatType.GROUP)
                .roomId(roomUuid)
                .sender("system")
                .senderName("시스템")
                .senderInitial(KoreanNameUtil.extractInitial("시스템")) // '시스템'의 초성을 추출하여 S로 설정
                .message(systemMessageText)
                .createdAt(LocalDateTime.now())
                .build();

        // WebSocket으로 브로드캐스팅: /sub/chat/room/{roomId}
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomUuid, systemMessage);

        log.info("[ChatRoomService] 채팅방 {}에 {}명의 멤버 추가 완료. 시스템 메시지 전송됨.", roomUuid, newRoomMembers.size());
    }

    /**
     * 1:1 채팅방 조회 또는 생성 (트랜잭션 필요)
     * 이미 존재하는 1:1 방이 있다면 그 방을 반환하고, 없다면 새로 생성합니다.
     */
    @Transactional
    public ChatRoomDto getOrCreatePersonalRoom(String myEmail, String targetEmail) {

        // 1. 참여자 두 명의 User 엔티티 조회
        User userA = userRepository.findByEmail(myEmail)
                .orElseThrow(() -> new IllegalArgumentException("요청자 유저를 찾을 수 없습니다: " + myEmail));
        User userB = userRepository.findByEmail(targetEmail)
                .orElseThrow(() -> new IllegalArgumentException("대상 유저를 찾을 수 없습니다: " + targetEmail));

        // 2. 기존 1:1 채팅방이 있는지 조회 (Repository에 해당 쿼리가 있다고 가정)
        Optional<ChatRoomEntity> existingRoom = chatRoomRepository
                .findPersonalRoomByMembers(userA, userB);

        if (existingRoom.isPresent()) {
            log.info("[Service] 기존 1:1 채팅방 발견: {}", existingRoom.get().getRoomId());
            // 3. 기존 방이 있으면 해당 방 DTO 반환
            return toDto(existingRoom.get());
        }

        // 4. 기존 방이 없으면 새로 생성
        log.info("[Service] 새로운 1:1 채팅방 생성 시작: {} <-> {}", myEmail, targetEmail);

        // 4-1. Entity 생성
        String uuid = UUID.randomUUID().toString();
        // 1:1 방의 이름은 상대방의 이름으로 표시되지만, DB에는 두 사람의 이메일 조합으로 저장하여 고유성 확보
        String roomName = userA.getUserName() + " & " + userB.getUserName();
        ChatRoomEntity entity = ChatRoomEntity.builder()
                .roomId(uuid)
                .roomName(roomName)
                .roomOwner(myEmail) // 생성자는 요청자
                .type(RoomType.PERSONAL)
                .build();

        ChatRoomEntity savedEntity = chatRoomRepository.save(entity);

        // 4-2. RoomMemberEntity 생성 및 저장
        List<User> initialMembers = List.of(userA, userB);
        List<RoomMemberEntity> roomMembers = initialMembers.stream()
                .map(user -> RoomMemberEntity.builder()
                        .room(savedEntity)
                        .user(user)
                        .build())
                .collect(Collectors.toList());

        roomMemberRepository.saveAll(roomMembers);

        // 5. 새로 생성된 방 DTO 반환
        return toDto(savedEntity);
    }

    @Transactional(readOnly = true)
    public List<String> getMemberEmails(UUID roomId) {
        String roomUuid = roomId.toString();

        // 1. 채팅방 엔티티 조회
        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomUuid)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. (ID: " + roomUuid + ")"));

        // 2. RoomMemberEntity를 통해 User 엔티티에 접근하여 이메일 목록을 추출
        List<RoomMemberEntity> members = roomMemberRepository.findByRoom(room);

        return members.stream()
                .map(roomMember -> roomMember.getUser().getEmail())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getParticipants(String roomId) {
        String roomUuid = roomId;

        // 1. 채팅방 엔티티 조회
        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomUuid)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. (ID: " + roomUuid + ")"));

        // 2. RoomMemberEntity를 통해 User 엔티티 조회
        List<RoomMemberEntity> members = roomMemberRepository.findByRoom(room);

        // 3. User 엔티티를 UserDto로 변환하여 반환
        return members.stream()
                .map(RoomMemberEntity::getUser) // 맵핑 간결화
                .map(user -> UserDto.builder()
                        .email(user.getEmail())
                        .userName(user.getUserName())
                        .phoneNumber(user.getPhoneNumber())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 채팅방에 속하지 않은 사용자 검색 (멤버 추가용)
     */
    @Transactional(readOnly = true)
    public List<UserDto> searchUsersNotInRoom(String query, String roomId) {
        // 1. 현재 채팅방 멤버 이메일 목록 조회
        List<String> memberEmails = getMemberEmails(UUID.fromString(roomId));

        // 2. 검색 쿼리로 사용자 조회
        List<User> foundUsers = userRepository.findByKeywordContaining(query);

        // 3. 멤버가 아닌 사용자만 필터링하여 DTO 변환
        return foundUsers.stream()
                .filter(user -> !memberEmails.contains(user.getEmail()))
                .map(user -> UserDto.builder()
                        .email(user.getEmail())
                        .userName(user.getUserName())
                        .phoneNumber(user.getPhoneNumber())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 특정 채팅방의 멤버 이메일 목록 조회 (헬퍼 메서드)
     */
    @Transactional(readOnly = true)
    public List<String> getMemberEmailsByRoomId(String roomId) {
        return getMemberEmails(UUID.fromString(roomId));
    }

    //특정 채팅방에 멤버를 추가하는 로직
    @Transactional
    public String addMemberToRoom(String roomId, String memberEmail) {

        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        User userToAdd = userRepository.findByEmail(memberEmail)
                .orElse(null);

        if (userToAdd == null) {
            return "해당 유저가 존재하지 않습니다.";
        }

        // 중복 체크
        Optional<RoomMemberEntity> existingMember = roomMemberRepository.findByRoomAndUser(room, userToAdd);

        if (existingMember.isPresent()) {
            return userToAdd.getEmail() + "님은 이미 채팅방 멤버입니다.";
        }

        // 멤버 추가
        RoomMemberEntity newMember = RoomMemberEntity.builder()
                .room(room)
                .user(userToAdd)
                .build();

        room.addRoomMember(newMember); // ChatRoomEntity의 addRoomMember가 RoomMember 목록과 UserCount를 적절히 갱신하는지 확인 필요
        roomMemberRepository.save(newMember);

        // [원래 코드 유지] entity.getUserCount()가 0일 경우 1로 처리
        return userToAdd.getEmail() + "님이 채팅방에 추가되었습니다. (현재 인원: " + (room.getUserCount() != 0 ? room.getUserCount() : 1) + ")";
    }

    @Transactional
    public void deleteRoom(UUID roomId) {
        String roomUuid = roomId.toString();

        // 1. ChatRoomEntity 조회
        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomUuid)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 채팅방을 찾을 수 없습니다. (ID: " + roomUuid + ")"));

        // 2. RoomMemberEntity 삭제
        roomMemberRepository.deleteAll(roomMemberRepository.findByRoom(room));

        // 🔑 [보강] ChatMessageEntity도 삭제해야 할 수 있음 (Cascade 설정에 따라 다름)
        // chatMessageRepository.deleteByRoomId(roomUuid);

        // 3. ChatRoomEntity 삭제
        chatRoomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDto> getRoomsByUserId(String userIdString) {
        Long userId;
        try {
            String processedIdString = userIdString.startsWith("user_") ?
                    userIdString.substring(5) : userIdString;

            userId = Long.parseLong(processedIdString);
            log.info("[DEBUG] Client ID {} processed to Long ID {}", userIdString, userId);

        } catch (NumberFormatException e) {
            log.error("[ERROR] Invalid userId format: {}", userIdString);
            return List.of();
        }

        User user = userRepository.findById(userId).orElse(null);

        if (user == null) {
            log.warn("[WARN] User not found for Long ID: {}", userId);
            return List.of();
        }

        //  1차 체크: RoomMember 엔티티 조회
        List<RoomMemberEntity> memberEntries = roomMemberRepository.findByUser(user);
        log.info("[DEBUG] Found {} RoomMember entries for user {}", memberEntries.size(), userId);

        if (memberEntries.isEmpty()) {
            log.warn("[WARN] User {} is not part of any room (RoomMember list is empty).", userId);
            return List.of();
        }

        // 2차 체크: ChatRoomDto 변환 및 안 읽은 메시지 수 계산
        List<ChatRoomDto> roomList = memberEntries.stream()
                .map(memberEntry -> {
                    // 1. LastReadTime 확인
                    LocalDateTime lastReadTime = memberEntry.getLastReadTime();
                    if (lastReadTime == null) {
                        lastReadTime = memberEntry.getJoinedAt();
                    }

                    // 2. 안 읽은 메시지 개수 조회 (DB 쿼리)
                    String roomId = memberEntry.getRoom().getRoomId();

                    // 버그 수정 적용: countByRoomIdAndCreatedAtGreaterThanEqual 사용
                    long unreadCount = chatMessageRepository
                            .countByRoomIdAndCreatedAtGreaterThanEqual(
                                    roomId,
                                    lastReadTime
                            );

                    // 3. DTO로 변환
                    return toDtoWithUnreadCount(memberEntry, unreadCount);
                })
                .collect(Collectors.toList());

        return roomList;
    }

    @Transactional
    public void updateLastReadTime(String roomId, String userEmail) {

        // 1. User 엔티티 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userEmail));
        // 2. ChatRoomEntity 조회
        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방 ID입니다: " + roomId));

        // 3. RoomMember 엔티티 조회
        RoomMemberEntity roomMember = roomMemberRepository
                .findByRoomAndUser(room, user)
                .orElseThrow(() -> new IllegalArgumentException("채팅방 멤버 정보를 찾을 수 없습니다. (Room: " + roomId + ", User: " + userEmail + ")"));

        // 4. LastReadTime 갱신
        roomMember.setLastReadTime(LocalDateTime.now());
        // 🔑 [복구] 명시적 저장 호출 (더 안전하게)
        roomMemberRepository.save(roomMember);
    }

    /**
     * 🔑 [추가된 메서드] 특정 사용자의 채팅방 마지막 읽은 시간을 조회합니다. (ISO 8601 문자열 반환)
     */
    @Transactional(readOnly = true)
    public String getLastReadTimeForUser(String roomId, String userEmail) {
        // 1. User 엔티티 조회
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 이메일입니다: " + userEmail));

        // 2. ChatRoomEntity 조회
        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방 ID입니다: " + roomId));

        // 3. RoomMember 엔티티 조회
        RoomMemberEntity roomMember = roomMemberRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 사용자는 채팅방의 멤버가 아닙니다. Room ID: " + roomId + ", User ID: " + user.getId()));

        // 4. LastReadTime 확인 및 반환
        LocalDateTime lastReadTime = roomMember.getLastReadTime();
        log.info("DB 저장 시간: {}",lastReadTime);
        // lastReadTime이 null일 경우 joinedAt을 사용 (초기 방 입장 시각)
        if (lastReadTime == null) {
            lastReadTime = roomMember.getJoinedAt();
        }
        log.info("ISO 변환 시간: {}",lastReadTime.toString());
        // 클라이언트(JavaScript)에서 처리하기 쉽도록 ISO 8601 문자열로 변환하여 반환
        return lastReadTime.toString();
    }


    /**
     * 1:1 채팅방 조회
     */
    public ChatRoomDto findPersonalRoom(String myId, String friendId) {
        // myId와 friendId를 사용하여 User 엔티티 조회
        User userA = userRepository.findByEmail(myId) // 또는 findById 사용
                .orElseThrow(() -> new IllegalArgumentException("사용자 A를 찾을 수 없습니다."));
        User userB = userRepository.findByEmail(friendId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 B를 찾을 수 없습니다."));

        return chatRoomRepository.findPersonalRoomByMembers(userA, userB)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * 1:1 채팅방 생성 (기존 createRoom 로직 활용 또는 별도 구현)
     */
    @Transactional
    public ChatRoomDto createPersonalRoom(String myId, String friendId) {
        // getOrCreatePersonalRoom 메서드 로직을 재사용하거나 분리
        return getOrCreatePersonalRoom(myId, friendId);
    }

    /**
     * 멤버 강퇴 로직
     * 채팅방 방장(roomOwner)만 강퇴 가능하며, 방장 본인은 강퇴할 수 없습니다.
     * 기존 removeMember(String roomId, Long userId)를 대체합니다.
     */
    @Transactional
    public void kickMember(String roomId, Long targetUserId, String kickerEmail) {
        ChatRoomEntity room = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 1. 강퇴 권한 확인 (요청자가 방장인지 확인)
        if (!room.getRoomOwner().equals(kickerEmail)) {
            // 방장 ID(이메일)와 강퇴 요청자 ID(이메일)가 다르면 권한 없음 예외 발생
            throw new AccessDeniedException("채팅방 방장만 멤버를 강퇴할 수 있습니다.");
        }

        // 2. 강퇴 대상 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("강퇴할 사용자를 찾을 수 없습니다."));

        // 3. 방장이 본인 강퇴 시도 방지
        if (targetUser.getEmail().equals(kickerEmail)) {
            throw new IllegalArgumentException("방장은 본인을 강퇴할 수 없습니다.");
        }

        // 4. RoomMemberEntity 조회 및 삭제
        RoomMemberEntity member = roomMemberRepository.findByRoomAndUser(room, targetUser)
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버가 방에 존재하지 않습니다."));

        roomMemberRepository.delete(member);
        log.info("[Kick Member] 방장 {}가 채팅방 {}에서 멤버 {}를 강퇴했습니다.", kickerEmail, roomId, targetUser.getEmail());
    }

    /**
     * 이 로직은 ChatMessageService로 위임됩니다.
     */
    @Transactional(readOnly = true)
    public ChatHistoryResponse loadChatHistory(String roomId, String cursor, String lastReadTimeStr, int limit) {
        return chatMessageService.loadChatHistory(roomId, cursor, lastReadTimeStr, limit);
    }

}