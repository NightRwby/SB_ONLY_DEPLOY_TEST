package com.example.demo.config.auth;


import com.example.demo.config.auth.provider.GoogleUserInfo;
import com.example.demo.config.auth.provider.KakaoUserInfo;
import com.example.demo.config.auth.provider.NaverUserInfo;
import com.example.demo.config.auth.provider.OAuth2UserInfo;
import com.example.demo.domain.dto.UserDto;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class PrincipalDetailsOAuth2Service extends DefaultOAuth2UserService
{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("");
        // 토큰 정보를 출력하는 기존 주석 처리된 코드는 제거했습니다.

        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println("oAuth2User : " + oAuth2User );
        System.out.println("oAuth2User.getAttributes() : " + oAuth2User.getAttributes() );
        System.out.println("Provider Name : " + userRequest.getClientRegistration().getClientName());

        String provider = userRequest.getClientRegistration().getClientName();
        Map<String,Object> attributes = oAuth2User.getAttributes();

        OAuth2UserInfo oAuth2UserInfo =null;
        String username=null;
        if(provider.startsWith("Kakao"))
        {
            Long id = (Long)attributes.get("id");

            // connected_at 처리를 위한 안전한 로직
            LocalDateTime connected_at = null;
            if(attributes.get("connected_at") != null) {
                try {
                    connected_at = OffsetDateTime.parse( attributes.get("connected_at").toString() ).toLocalDateTime();
                } catch (Exception e) {
                    log.error("Kakao connected_at parsing failed: {}", e.getMessage());
                }
            }

            Map<String,Object> properties = (Map<String,Object>)attributes.get("properties");
            Map<String,Object> kakao_account = (Map<String,Object>)attributes.get("kakao_account");

            oAuth2UserInfo = KakaoUserInfo.builder()
                    .id(id)
                    .connected_at(connected_at)
                    .properties(properties)
                    .kakao_account(kakao_account)
                    .build();

            // DB 등록예정 계정명 (username은 provider_providerId로 설정)
            username = oAuth2User.getName();


        }
        else if(provider.startsWith("Naver"))
        {
            Map<String,Object> response = (Map<String,Object>)attributes.get("response");
            oAuth2UserInfo = NaverUserInfo.builder()
                    .response(response)
                    .build();

            // DB 등록예정 계정명 (Naver는 Email을 username으로 사용)
            username = oAuth2UserInfo.getEmail();
        }

        else if(provider.startsWith("Google"))
        {
            oAuth2UserInfo = GoogleUserInfo.builder()
                    .attributes(attributes)
                    .build();

            // DB 등록예정 계정명 (Google은 Email을 username으로 사용)
            username = oAuth2UserInfo.getEmail();
        }
        System.out.println("oAuth2UserInfo : " + oAuth2UserInfo);

        // OAuth2정보 -> 로컬계정생성(계정x : 생성 , 계정o : 불러오기)
        String password= passwordEncoder.encode("1234");
        String uniquePhoneNumber = oAuth2UserInfo.getProvider() + "_" + oAuth2UserInfo.getProviderId();

        // 기존 계정 존재 여부에 따라 DB저장/로드
        Optional<User> userOptional = userRepository.findByUserName(username);

        User loadedUser = null;

        if(userOptional.isEmpty()){
            // 신규 사용자 등록
            User newUser = new User();
            newUser.setUserName(username);
            newUser.setRole("ROLE_USER");
            newUser.setPassWord("123");
            newUser.setEmail(oAuth2UserInfo.getEmail());
            userRepository.save(newUser);
            loadedUser = newUser;
        }else{
            // 기존 사용자 로드
            loadedUser = userOptional.get();
        }

        // UserDto를 Setter를 사용하여 명확하게 생성
        UserDto dto = new UserDto();
        dto.setUserName(loadedUser.getUserName());
        dto.setPassWord(loadedUser.getPassword()); // DB의 암호화된 PW
        dto.setRole(loadedUser.getRole());         // ROLE_USER
        dto.setPhoneNumber(loadedUser.getPhoneNumber());
        dto.setEmail(loadedUser.getEmail());

        // 추가 속성 설정
        dto.setProvider(provider);
        dto.setProviderId(oAuth2UserInfo.getProviderId());

        PrincipalDetails principalDetails = new PrincipalDetails(dto, oAuth2UserInfo.getAttributes());

        // 추가된 출력 코드
        System.out.println("FINAL PrincipalDetails: " + principalDetails);

        return principalDetails;// dto , attributes 전달
    }
}