package com.example.demo.domain.view;

import com.example.demo.domain.entity.user.User;
import lombok.Value;

@Value
public class LoginUserView {
    String userName;
    String email;
    String nickName;
    String profileImageUrl;

    public static LoginUserView from(User user){
        return new LoginUserView(user.getUserName(), user.getEmail(), user.getNickName(), user.getProfileImageUrl());
    }

}