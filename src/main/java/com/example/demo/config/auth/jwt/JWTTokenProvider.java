package com.example.demo.config.auth.jwt;

import com.example.demo.config.auth.PrincipalDetails;
import com.example.demo.domain.dto.UserDto;
import com.example.demo.domain.entity.Signature;
import com.example.demo.domain.repository.SignatureRepository;
import com.example.demo.domain.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JWTTokenProvider {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SignatureRepository signatureRepository;



    //Key
    private Key key ;

    public Key getKey(){
        return key;
    }
    @PostConstruct
    public void init(){
        List<Signature> list = signatureRepository.findAll();
        if(list.isEmpty()){
            byte[] keyBytes = KeyGenerator.keyGen();
            this.key = Keys.hmacShaKeyFor(keyBytes);

            Signature signature = new Signature();
            signature.setKeyBytes(keyBytes);
            signature.setCreateAt(LocalDate.now());
            signatureRepository.save(signature);

        }else{
            Signature signature = list.get(0);
            this.key = Keys.hmacShaKeyFor(signature.getKeyBytes());
        }

    }

    public TokenInfo generateToken(Authentication authentication){

        //계정정보 - 계정명 / auth(role)
        String authorities = authentication  .getAuthorities()// Collection<SimpleGrantedAuthority> authorities 반환
                .stream()   // Stream 함수 사용예정
                .map((role)->{return role.getAuthority();}) // 각각 GrantedAuthoriy("ROLE~")들을 문자열값으로 반환해서 map처리
                .collect(Collectors.joining(",")); //각각의 role(ROLE_ADMIN ROLE_USER...) 를 ','를 기준으로 묶음 ("ROLE_USER,ROLE_ADMIN")
        //AccessToken(서버의 서비스를 이용제한 )
        System.out.println("토큰발급 : "+authentication);
        System.out.println("토큰발급 : "+authorities);

        long now = (new Date()).getTime();  //현재시간
        String accessToken = Jwts.builder()
                .setSubject(authentication.getName()) //본문 TITLE
                .setExpiration(new Date(now + JWTProperties.ACCESS_TOKEN_EXPIRATION_TIME )) //만료날짜(밀리초단위)
                .signWith(key, SignatureAlgorithm.HS256) // 서명값
                .claim("email",authentication.getName()) // 본문 내용
                .claim("auth",authorities) // 본문 내용
                .compact();
        //RefreshToken(AccessToken 만료시 갱신처리)
        String refreshToken = Jwts.builder()
                .setSubject("Refresh_Token_Title") //본문 TITLE
                .setExpiration(new Date(now + JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME )) //만료날짜(밀리초단위)
                .signWith(key, SignatureAlgorithm.HS256) // 서명값
                .compact();

        //TokenInfo
        return TokenInfo.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public Authentication getAuthentication(String accessToken) throws ExpiredJwtException
    {
        Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(accessToken).getBody();

        String email = claims.getSubject(); // email
        email = (String)claims.get("email"); //email
        String auth = (String)claims.get("auth"); //"ROLE_USER,ROLE_ADMIN"

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        String roles [] = auth.split(","); //["ROLE_ADMIN","ROLE_USER"]
        for(String role : roles){
            authorities.add(new SimpleGrantedAuthority(role));
        }


        PrincipalDetails principalDetails = null;
        UserDto dto = null;
        if(userRepository.existsByEmail(email)){

            dto = new UserDto();
            dto.setEmail(email);
            dto.setRole(auth);
            dto.setPassWord(null);

            principalDetails = new PrincipalDetails(dto);
        }

        if(principalDetails!=null) {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(principalDetails, null, authorities);
            return authenticationToken;
        }

        return null;


    }

    public boolean validateToken(String token) throws Exception
    {
        boolean isValid = false;
        try {
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token);
            isValid = true;
        }catch(ExpiredJwtException e) {
            // 토큰 만료시 예외
            log.info("[ExpiredJwtException].." + e.getMessage());
            throw new ExpiredJwtException(null,null,null); //header,claims,message
        }
        return isValid;

        // SecurityException	서명 불일치	변조된 토큰 또는 잘못된 키
        // MalformedJwtException 형식 오류	JWT 구조(header.payload.signature) 깨짐
        // ExpiredJwtException	만료된 토큰	exp 클레임이 현재 시간 이전
        // UnsupportedJwtException	지원되지 않는 형식	비표준 JWT, 미지원 알고리즘
        // IllegalArgumentException	잘못된 입력	null 또는 빈 토큰
    }


    public String getExpiredEmail(String token) throws ExpiredJwtException {
        try {
            // 일반적인 파싱 시도 (만료되었으면 ExpiredJwtException 발생)
            Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();
            return null; // 만료되지 않았으면 null 반환하거나 다른 예외 처리
        } catch (ExpiredJwtException e) {
            // ExpiredJwtException 발생 시, Claims만 추출
            Claims claims = e.getClaims();
            // subject 또는 "email" 클레임 사용
            return claims.getSubject();
        }
    }
    public TokenInfo reissueTokens(String email, String authorities){

        long now = (new Date()).getTime();

        // 1. New Access Token
        String newAccessToken = Jwts.builder()
                .setSubject(email)
                .setExpiration(new Date(now + JWTProperties.ACCESS_TOKEN_EXPIRATION_TIME ))
                .signWith(key, SignatureAlgorithm.HS256)
                .claim("email",email)
                .claim("auth",authorities)
                .compact();

        // 2. New Refresh Token (Rolling Refresh Token)
        String newRefreshToken = Jwts.builder()
                .setSubject("Refresh_Token_Title")
                .setExpiration(new Date(now + JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // 3. Return New TokenInfo
        return TokenInfo.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    //AccessToken 문자열 추출 (회원탈퇴)
    public String resolveToken(HttpServletRequest request){

        String bearerToken = request.getHeader("Authorization");

        if (bearerToken !=null && bearerToken.startsWith("Bearer")){
            return bearerToken.substring(7);
        }
        return null;
    }

    public Long getRemainingExpiration(String token){

        try{
            //토큰 만료시간포함 추출
            Claims claims = Jwts.parser().setSigningKey(key).build().parseClaimsJws(token).getBody();

            //만료시간 - 현재시간
            Long expirationTime = claims.getExpiration().getTime();
            Long now = (new Date()).getTime();

            //남은 유효시간 반환
            return expirationTime - now;
        }catch (Exception e){
            //유효하지않은 토큰이면 0또는 음수반환
            log.error("Error calculating remaining expiration for token: {}",e.getMessage());
            return 0L;
        }
    }

}