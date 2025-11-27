package com.example.demo.domain.repository;

import com.example.demo.domain.entity.CommunityImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityImageRepository extends JpaRepository<CommunityImageEntity, Long> {
}