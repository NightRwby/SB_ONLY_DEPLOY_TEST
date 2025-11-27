package com.example.demo.domain.entity.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@Entity
@Table(name="user")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;
    @Email(message = "example@example.com 형식으로 작성해주세요.")
    @Column(unique = true ,nullable = false)
    private String email;
    @Column(nullable = false)
    private String passWord;
    @Column(unique = true,nullable = false)
    private String phoneNumber;
    private String role;
    private String userName;

    //테이블관계
    @OneToOne(mappedBy = "user",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private UserProfile userProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserAppSetting userAppSetting;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserBlocking> blockingList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private  List<UserDeviceHistory> HistoryList = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserScrap> ScrapList = new ArrayList<>();


    public String getNickName(){
        if(userProfile.getNickName() == null)
            return this.userName;
        return userProfile.getNickName();
    }

    public void updatePassword(String newEncodedPassword){
        this.passWord = newEncodedPassword;
    }


    //UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {return List.of();}
    @Override
    public String getPassword() {return this.passWord;}
    @Override
    public String getUsername() {return this.email;}

    @Override
    public boolean isAccountNonExpired() {
        return this.userProfile != null && this.userProfile.isAccountNonExpired();}

    @Override
    public boolean isAccountNonLocked() {
        return this.userProfile != null && this.userProfile.isAccountNonLocked();}

    @Override
    public boolean isCredentialsNonExpired() {
        return this.userProfile != null && this.userProfile.isCredentialsNonExpired();}

    @Override
    public boolean isEnabled() {
        return this.userProfile != null && this.userProfile.isEnabled();}


    public String getUserName() {
        return this.userName;
    }

    public String getProfileImageUrl() {
        return this.userProfile.getProfileImageUrl();
    }

    public void updateUserProfile(UserProfile userProfile){
        this.userProfile = userProfile;
    }
}