package com.example.demo.domain.repository;

import com.example.demo.domain.entity.user.UserBlocking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockListRepository extends JpaRepository<UserBlocking,Long> {

    //누가 차단했는지 조회
    List<UserBlocking> findByUser_Email(String email);

    //누가(user.email) 누구를(blockedUserEmail) 차단했는지 확인
    boolean existsByUser_EmailAndBlockedUserEmail(String userEmail,String blockedEmail);

    //내가 차단당했는지 확인
    List<UserBlocking> findByBlockedUserEmail(String blockedEmail);

    List<UserBlocking> findByUserEmail(String email);

    Optional<UserBlocking> findByUser_EmailAndBlockedUserEmail(String userEmail,String blockedEmail);




}
