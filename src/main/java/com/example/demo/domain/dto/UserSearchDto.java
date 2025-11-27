package com.example.demo.domain.dto;


import com.example.demo.domain.entity.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDto {

    private String email;
    private String userName;

    public UserSearchDto(User user){
        this.email = user.getEmail();
        this.userName = user.getUsername();
    }


}
