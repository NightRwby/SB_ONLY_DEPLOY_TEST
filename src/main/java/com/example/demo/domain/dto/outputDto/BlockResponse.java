package com.example.demo.domain.dto.outputDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockResponse {


    private String blockedUserEmail;
    private LocalDateTime blockedAt;
    private String blockedUserNickname;

}
