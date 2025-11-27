package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "Community")
public class CommunityEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String writer;
    private String menuId;
    private String category;

    private int views;
    private int likes;

    // 3. 파일 경로 저장용 필드 (여러 개일 경우 콤마로 구분하여 저장 예정)
    @Column(columnDefinition = "TEXT")
    private String filepath;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @Builder
    public CommunityEntity(String title, String content, String writer, String menuId, String category, String filepath, int views, int likes) {
        this.title = title;
        this.content = content;
        this.writer = writer;
        this.menuId = menuId;
        this.category = category;
        this.filepath = filepath; // 빌더 추가
        this.views = 0;
        this.likes = 0;
    }
}