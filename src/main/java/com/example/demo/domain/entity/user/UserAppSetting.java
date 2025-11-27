package com.example.demo.domain.entity.user;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="UserAppSetting")
@Builder
public class UserAppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email",referencedColumnName = "email",unique = true,nullable = false)
    private User user;

    private String language;
    private String theme;
    private  boolean linkPreviewEnabled;
    private  String notificationSound;
    private  String timeFormat;
    private  boolean compactModeEnabled;
    private  boolean mediaAutoplayEnabled;

}
