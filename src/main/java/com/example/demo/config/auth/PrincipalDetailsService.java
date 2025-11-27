package com.example.demo.config.auth;

import com.example.demo.domain.dto.UserDto;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class PrincipalDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println("PrincipalDetailsService's loadUserByUsername : " + email);

        Optional<User> userOptional =
                userRepository.findByEmail(email);
        if(userOptional.isEmpty())
            throw new UsernameNotFoundException(email+" 계정이 존재하지 않습니다");

        //ENTITY -> DTO
        User user = userOptional.get();
        UserDto dto = new UserDto();
        dto.setUserName(user.getUserName());
        dto.setPassWord(user.getPassword());
        dto.setRole(user.getRole());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setEmail(user.getEmail());

        PrincipalDetails principalDetails = new PrincipalDetails(dto);


        System.out.println("PrincipalDetails Info: " + principalDetails);

        return new PrincipalDetails(dto);


    }
}