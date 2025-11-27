package com.example.demo.domain.repository;

import com.example.demo.domain.entity.CommunityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<CommunityEntity, Long> {
    // menuId(게시판 종류)로 조회
    Page<CommunityEntity> findByMenuId(String menuId, Pageable pageable);

    // menuId 안에서 검색
    Page<CommunityEntity> findByMenuIdAndTitleContaining(String menuId, String keyword, Pageable pageable);

    // menuId 안에서 특정 카테고리(말머리) 필터링
    Page<CommunityEntity> findByMenuIdAndCategory(String menuId, String category, Pageable pageable);
}