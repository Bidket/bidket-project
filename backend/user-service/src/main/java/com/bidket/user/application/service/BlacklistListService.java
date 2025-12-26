package com.bidket.user.application.service;

import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import com.bidket.user.infrastructure.persistence.repository.UserBlacklistRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.BlacklistItemResponse;
import com.bidket.user.presentation.dto.response.BlacklistListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 블랙리스트 목록 조회 서비스
 * 관리자가 블랙리스트 목록을 조회합니다.
 */
@Service
@RequiredArgsConstructor
public class BlacklistListService {

    private final UserBlacklistRepository userBlacklistRepository;
    private final UserRepository userRepository;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE = 0;

    /**
     * 블랙리스트 목록 조회
     * @param page 페이지 번호 (0부터 시작, 컨트롤러에서 기본값 0 설정)
     * @param size 페이지 사이즈 (컨트롤러에서 기본값 20 설정, 최대 100)
     * @param activeOnly 현재 유효한 블랙리스트만 조회할지 여부 (컨트롤러에서 기본값 true 설정)
     * @return 블랙리스트 목록 조회 응답 (페이지네이션 포함)
     */
    @Transactional(readOnly = true)
    public BlacklistListResponse getBlacklistList(Integer page, Integer size, Boolean activeOnly) {
        // 컨트롤러에서 기본값과 유효성 검증을 처리하므로, 여기서는 null이 오지 않음
        // 하지만 방어적 프로그래밍을 위해 null 체크 및 범위 검증 유지
        int pageNumber = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int pageSize = (size != null && size >= 1 && size <= 100) ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        // activeOnly 기본값 true
        boolean filterActiveOnly = activeOnly != null ? activeOnly : true;
        
        // 현재 시간 (만료 여부 확인용)
        LocalDateTime now = LocalDateTime.now();
        
        // 블랙리스트 목록 조회
        Page<UserBlacklist> blacklistPage = userBlacklistRepository.findAllWithFilter(filterActiveOnly, now, pageable);
        
        // User 정보를 한 번에 조회하기 위해 userId 수집
        Set<UUID> userIds = blacklistPage.getContent().stream()
                .map(UserBlacklist::getUserId)
                .collect(Collectors.toSet());
        
        // User 정보 조회 (Map으로 변환하여 빠른 조회)
        // 빈 Set인 경우 불필요한 DB 조회 방지
        Map<UUID, User> userMap = userIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user));
        
        // 응답 DTO 변환
        List<BlacklistItemResponse> content = blacklistPage.getContent().stream()
                .map(blacklist -> toItemResponse(blacklist, userMap.get(blacklist.getUserId())))
                .collect(Collectors.toList());
        
        return BlacklistListResponse.builder()
                .content(content)
                .totalElements(blacklistPage.getTotalElements())
                .totalPages(blacklistPage.getTotalPages())
                .page(blacklistPage.getNumber())
                .size(blacklistPage.getSize())
                .build();
    }

    /**
     * UserBlacklist 엔티티를 BlacklistItemResponse로 변환
     */
    private BlacklistItemResponse toItemResponse(UserBlacklist blacklist, User user) {
        return BlacklistItemResponse.builder()
                .memberId(blacklist.getUserId())
                .nickname(user != null ? user.getNickname() : null)
                .reason(blacklist.getReason())
                .expireAt(blacklist.getExpireAt())
                .createdAt(blacklist.getCreatedAt())
                .active(blacklist.getActive())
                .build();
    }
}

