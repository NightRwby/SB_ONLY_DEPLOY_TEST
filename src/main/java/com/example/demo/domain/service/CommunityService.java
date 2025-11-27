package com.example.demo.domain.service;
import com.example.demo.domain.entity.CommunityEntity;
import com.example.demo.domain.entity.CommunityImageEntity;
import com.example.demo.domain.repository.CommunityImageRepository;
import com.example.demo.domain.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityImageRepository communityImageRepository; // 추가

    // 목록 조회 (기존 유지)
    @Transactional(readOnly = true)
    public Page<CommunityEntity> getBoardList(String menuId, String keyword, String category, String sort, int page) {
        Sort sorting = Sort.by(Sort.Direction.DESC, "createdDate");
        if ("views".equals(sort)) sorting = Sort.by(Sort.Direction.DESC, "views");
        else if ("likes".equals(sort)) sorting = Sort.by(Sort.Direction.DESC, "likes");

        Pageable pageable = PageRequest.of(page - 1, 12, sorting);

        if (keyword != null && !keyword.isEmpty()) {
            if (keyword.startsWith("#")) {
                String tag = keyword.replace("#", "").trim();
                return communityRepository.findByMenuIdAndCategory(menuId, tag, pageable);
            } else {
                return communityRepository.findByMenuIdAndTitleContaining(menuId, keyword, pageable);
            }
        } else if (category != null && !category.isEmpty()) {
            return communityRepository.findByMenuIdAndCategory(menuId, category, pageable);
        } else {
            return communityRepository.findByMenuId(menuId, pageable);
        }
    }

    // [수정됨] 글쓰기 (DB에 이미지 저장)
    @Transactional
    public void writeBoard(String title, String content, String menuId, String category, String writer, List<MultipartFile> files) throws IOException {

        StringBuilder imageUrls = new StringBuilder();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    // 1. DB에 이미지 저장
                    CommunityImageEntity img = CommunityImageEntity.builder()
                            .originalFileName(file.getOriginalFilename())
                            .imageData(file.getBytes()) // 바이너리 데이터
                            .build();
                    CommunityImageEntity savedImg = communityImageRepository.save(img);

                    // 2. 이미지를 불러올 URL 생성 (예: /community/image/view/15)
                    if (imageUrls.length() > 0) imageUrls.append(",");
                    imageUrls.append("/community/image/view/").append(savedImg.getId());
                }
            }
        }

        CommunityEntity board = CommunityEntity.builder()
                .title(title)
                .content(content)
                .menuId(menuId)
                .category(category)
                .writer(writer)
                .filepath(imageUrls.toString()) // URL 주소 저장
                .build();
        communityRepository.save(board);
    }

    // [추가] 이미지 데이터 가져오기 (컨트롤러에서 사용)
    @Transactional(readOnly = true)
    public CommunityImageEntity getImage(Long id) {
        return communityImageRepository.findById(id).orElse(null);
    }

    // [수정됨] 게시글 수정 (파일 추가 업로드 포함) - 삭제 로직 개선
    @Transactional
    public void updateBoard(Long id, String title, String content, String category, List<MultipartFile> files, boolean clearExistingFiles) throws IOException {
        CommunityEntity board = communityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        // 1. 텍스트 정보 수정
        board.setTitle(title);
        board.setContent(content);
        board.setCategory(category);

        // 2. [핵심 수정] 최종 경로 빌더 초기화
        StringBuilder finalPaths = new StringBuilder();

        // 2-1. 삭제 요청이 없고(false), 기존 경로가 있으면 finalPaths에 유지
        if (!clearExistingFiles && board.getFilepath() != null && !board.getFilepath().isEmpty()) {
            finalPaths.append(board.getFilepath());
        }
        // (만약 clearExistingFiles=true이면, finalPaths는 비어있게 시작합니다.)

        // 2-2. 새 파일이 있으면 저장 후 경로 빌더에 추가
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    CommunityImageEntity img = CommunityImageEntity.builder()
                            .originalFileName(file.getOriginalFilename())
                            .imageData(file.getBytes())
                            .build();
                    CommunityImageEntity savedImg = communityImageRepository.save(img);

                    // 기존 경로가 있다면 콤마로 구분, 아니면 바로 추가
                    if (finalPaths.length() > 0) finalPaths.append(",");
                    finalPaths.append("/community/image/view/").append(savedImg.getId());
                }
            }
        }

        // 3. 최종 경로 업데이트 (이후 로직에서는 이 값만 사용)
        // -> clearExistingFiles=true 이고 새 파일이 없으면 최종적으로 "" 빈 문자열로 저장됩니다.
        board.setFilepath(finalPaths.toString());
    }

    @Transactional
    public CommunityEntity getBoardDetail(Long id) {
        CommunityEntity board = communityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다."));
        board.setViews(board.getViews() + 1);
        return board;
    }

    @Transactional
    public void deleteBoard(Long id) {
        communityRepository.deleteById(id);
    }

    @Transactional
    public int updateLikes(Long id, boolean isUp) {
        CommunityEntity board = communityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));
        if (isUp) board.setLikes(board.getLikes() + 1);
        else if (board.getLikes() > 0) board.setLikes(board.getLikes() - 1);
        return board.getLikes();
    }
}