package com.example.demo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// 복합키는 Serializable, equals, hashCode를 구현해야 합니다.
@Data // EqualsAndHashCode 포함
@NoArgsConstructor
@AllArgsConstructor // 💡 이 생성자가 JPA에 필요합니다.
public class RoomMemberId implements Serializable {
    private Long room;
    private Long user;
}
