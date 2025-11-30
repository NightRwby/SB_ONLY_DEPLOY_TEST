package com.example.demo.domain.repository;

import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.entity.user.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    Optional<User> findByUserEmail(String email);

}