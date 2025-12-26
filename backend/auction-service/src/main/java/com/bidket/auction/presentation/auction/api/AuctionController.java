package com.bidket.auction.presentation.auction.api;

import com.bidket.auction.application.auction.dto.request.CreateAuctionRequest;
import com.bidket.auction.application.auction.dto.request.UpdateAuctionRequest;
import com.bidket.auction.application.auction.dto.response.AuctionResponse;
import com.bidket.auction.application.auction.service.AuctionService;
import com.bidket.auction.domain.auction.model.AuctionStatus;
import com.bidket.common.presentation.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Auction API", description = "경매 관리 API")
@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Slf4j
public class AuctionController {

    private final AuctionService auctionService;

    @Operation(summary = "경매 생성", description = "새로운 경매를 생성합니다")
    @PostMapping
    public ResponseEntity<ApiResponse<AuctionResponse>> createAuction(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateAuctionRequest request) {
        log.info("[경매 생성 API] userId={}, 요청: {}", userId, request);

        AuctionResponse response = auctionService.createAuction(userId, request);

        log.info("[경매 생성 API] 성공: auctionId={}", response.id());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("경매가 생성되었습니다", response));
    }

    @Operation(summary = "경매 상세 조회", description = "경매 ID로 상세 정보를 조회합니다")
    @GetMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> getAuction(
            @Parameter(description = "경매 ID", required = true)
            @PathVariable UUID auctionId) {
        log.info("[경매 조회 API] auctionId={}", auctionId);

        AuctionResponse response = auctionService.getAuction(auctionId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "판매자의 경매 목록 조회", description = "특정 판매자의 모든 경매를 조회합니다")
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<AuctionResponse>>> getAuctionsBySeller(
            @Parameter(description = "판매자 ID", required = true)
            @PathVariable UUID sellerId) {
        log.info("[판매자 경매 목록 API] sellerId={}", sellerId);

        List<AuctionResponse> responses = auctionService.getAuctionsBySeller(sellerId);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "상태별 경매 목록 조회", description = "특정 상태의 경매 목록을 페이징하여 조회합니다")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuctionResponse>>> getAuctionsByStatus(
            @Parameter(description = "경매 상태 (선택)", example = "ACTIVE")
            @RequestParam(required = false) AuctionStatus status,
            @Parameter(description = "페이지 번호 (0부터 시작, 최소 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최소 1, 최대 100, 기본 20)", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬 기준", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sort,
            @Parameter(description = "정렬 방향 (asc/desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String direction) {
        log.info("[상태별 경매 목록 API] status={}, page={}, size={}", status, page, size);

        // 상태 기본값 설정
        if (status == null) {
            status = AuctionStatus.ACTIVE;
        }

        // 페이지 번호 검증 (음수 방지)
        if (page < 0) {
            page = 0;
            log.warn("[상태별 경매 목록 API] 페이지 번호가 음수입니다. 0으로 설정합니다.");
        }

        // 페이지 크기 검증 (최소 1, 최대 100)
        if (size < 1) {
            size = 1;
            log.warn("[상태별 경매 목록 API] 페이지 크기가 1 미만입니다. 1로 설정합니다.");
        } else if (size > 100) {
            size = 100;
            log.warn("[상태별 경매 목록 API] 페이지 크기가 100을 초과합니다. 100으로 제한합니다.");
        }

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        Page<AuctionResponse> responses = auctionService.getAuctionsByStatusWithPaging(status, pageable);

        log.info("[상태별 경매 목록 API] 조회 완료: totalElements={}, totalPages={}",
                responses.getTotalElements(), responses.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @Operation(summary = "경매 수정", description = "경매 정보를 수정합니다 (PENDING 상태만)")
    @PutMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> updateAuction(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "경매 ID", required = true)
            @PathVariable UUID auctionId,
            @Valid @RequestBody UpdateAuctionRequest request) {
        log.info("[경매 수정 API] userId={}, auctionId={}, request={}", userId, auctionId, request);

        AuctionResponse response = auctionService.updateAuction(userId, auctionId, request);

        log.info("[경매 수정 API] 성공: auctionId={}", auctionId);

        return ResponseEntity.ok(ApiResponse.success("경매가 수정되었습니다", response));
    }

    @Operation(summary = "경매 취소", description = "경매를 취소합니다 (입찰 없을 때만)")
    @DeleteMapping("/{auctionId}")
    public ResponseEntity<ApiResponse<Void>> cancelAuction(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "경매 ID", required = true)
            @PathVariable UUID auctionId) {
        log.info("[경매 취소 API] userId={}, auctionId={}", userId, auctionId);

        auctionService.cancelAuction(userId, auctionId);

        log.info("[경매 취소 API] 성공: auctionId={}", auctionId);

        return ResponseEntity.ok(ApiResponse.success("경매가 취소되었습니다", null));
    }

    @Operation(summary = "경매 확정 (내부 API)", description = "재고 예약 완료 후 경매를 확정합니다")
    @PostMapping("/{auctionId}/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmAuctionCreation(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader("X-User-Id") UUID userId,
            @Parameter(description = "경매 ID", required = true)
            @PathVariable UUID auctionId) {
        log.info("[경매 확정 API] userId={}, auctionId={}", userId, auctionId);

        auctionService.confirmAuctionCreation(userId, auctionId);

        log.info("[경매 확정 API] 성공: auctionId={}", auctionId);

        return ResponseEntity.ok(ApiResponse.success("경매가 확정되었습니다", null));
    }
}
