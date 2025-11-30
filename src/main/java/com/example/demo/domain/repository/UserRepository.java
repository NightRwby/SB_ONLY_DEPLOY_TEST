package com.example.demo.domain.repository;

import com.example.demo.domain.entity.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUserName(String userName);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String Email);
    void deleteByEmail(String email);

    List<User> findByEmailIn(List<String> emails);

    @Query("SELECT u FROM User u WHERE " +
            "u.userName LIKE %:keyword% OR " +
            "u.email LIKE %:keyword% OR " +
            "u.phoneNumber LIKE %:keyword%")
    List<User> findByKeywordContaining(@Param("keyword") String keyword);

    List<User> findByUserNameContainingOrEmailContaining(String userNameQuery,String emailQuery);
}
