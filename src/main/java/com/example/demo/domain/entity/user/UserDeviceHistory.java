package com.example.demo.domain.entity.user;


import com.example.demo.domain.enums.DeviceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "UserDeviceHistory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type",nullable = false,length = 10)
    private DeviceType deviceType;
    @Column(name = "ip_address", nullable = false,length = 45)
    private String ipAddress;
    @Column(name = "last_login_at",nullable = false)
    private LocalDateTime lastLoginAt;
}
