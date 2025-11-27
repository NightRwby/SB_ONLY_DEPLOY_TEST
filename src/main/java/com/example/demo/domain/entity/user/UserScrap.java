package com.example.demo.domain.entity.user;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserScrap")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserScrap {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email", nullable = false)
    private User user;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long contentId;

    @Column(nullable = false)
    private LocalDateTime scrappedAt;
}
