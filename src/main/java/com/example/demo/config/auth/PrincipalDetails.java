package com.example.demo.config.auth;

import com.example.demo.domain.dto.UserDto;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Data
public class PrincipalDetails implements UserDetails, OAuth2User {

    private UserDto dto;

    //OAUTH 속성
    Map<String, Object> attributes;

    public PrincipalDetails(UserDto dto) {
        this.dto = dto;
    }

    // 2. OAuth2 로그인
    public PrincipalDetails(UserDto dto, Map<String, Object> attributes) {
        this.dto = dto;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (dto.getRole() == null || dto.getRole().isEmpty()) {
            return Collections.emptyList();
        }
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        String roles [] = dto.getRole().split(",");
        for(String role : roles){
            authorities.add(new SimpleGrantedAuthority(role.trim()));
        }

        return authorities;
    }

    //로컬 인증
    @Override
    public String getPassword() {
        return dto.getPassWord();
    }
    @Override
    public String getUsername() {
        return dto.getEmail();
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


    // OAUTH2 메서드: 소셜 인증에 사용
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return dto.getUserName();
    }

    public String getEmail() {
        return dto.getEmail();
    }
}