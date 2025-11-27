package com.example.demo.domain.entity.user;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="UserProfile")
@Builder
public class UserProfile {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // UserProfile 자체의 PK


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email", referencedColumnName = "email", unique = true, nullable = false)
    private User user; // FK를 관리할 User 객체 필드

    private String nickName;
    private String profileImageUrl;
    private String bio;
    private boolean readReceiptsEnabled;
    @Column(nullable = false)
    private boolean isAccountNonExpired;
    @Column(nullable = false)
    private boolean isAccountNonLocked;
    @Column(nullable = false)
    private boolean isEnabled;
    @Column(nullable = false)
    private boolean isCredentialsNonExpired;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private  String businessEmail;
}
