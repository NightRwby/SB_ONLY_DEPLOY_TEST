package com.example.demo.domain.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.eclipse.tags.shaded.org.apache.xpath.operations.Bool;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryEntity {

    @Column(name = "status")
    private Boolean status = false;

    @NotBlank(message = "문의 유형을 확인해주세요.")
    @Column(nullable = false, length = 30)
    private String inquiryType;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "문의 제목을 확인해주세요.")
    @Size(max = 25, message = "제목 최대 25자 입력")
    @Column(nullable = false, length = 25)
    private String title;

    @NotBlank(message = "문의 내용을 확인해주세요.")
    @Size(max = 500, message = "내용 최대 500자 입력")
    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


}
