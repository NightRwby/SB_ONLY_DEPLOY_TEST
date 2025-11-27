package com.example.demo.domain.entity.user;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name="UserBlocking")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserBlocking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // UserBlocking 자체 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email", nullable = false)
    private User user;

    @Column(nullable = false)
    private String blockedUserEmail;
    @Column(nullable = false)
    private LocalDateTime blockedAt;
}
