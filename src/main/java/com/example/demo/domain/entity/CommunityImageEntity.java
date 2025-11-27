package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "CommunityImage")
public class CommunityImageEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originalFileName;

    @Lob // 대용량 데이터 저장 (BLOB)
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Builder
    public CommunityImageEntity(String originalFileName, byte[] imageData) {
        this.originalFileName = originalFileName;
        this.imageData = imageData;
    }
}