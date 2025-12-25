package com.bidket.queue.domain.model;

import com.bidket.common.presentation.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QueueErrorCode implements BaseErrorCode {
    REDIS_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 연결 오류 발생"),
    REDIS_EXPIRE_SET_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TTL 설정 중 오류 발생"),
    REDIS_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 저장 실패"),

    INVALID_TOKEN(HttpStatus.FORBIDDEN, "유효하지 않은 Active Token"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "저장된 토큰이 없습니다."),
    CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "저장된 설정이 없습니다."),
    AUCTION_REGISTER_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "경매 캐싱 등록 실패"),

    AUCTION_CLOSED(HttpStatus.FORBIDDEN, "이미 종료된 경매입니다."),
    AUCTION_NOT_OPENED(HttpStatus.FORBIDDEN, "경매 오픈 전입니다."),

    WAITING_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "대기열에 사용자 정보가 존재하지 않습니다."),

    INVALID_UPDATE_FIELD(HttpStatus.INTERNAL_SERVER_ERROR, "수정 필드는 전부 null일 수 없습니다."),

    QUEUE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "대기열 삭제 에러 발생"),
    NOTIFICATION_EVENT_PUBLISH_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "대기열 이벤트 발행 실패"),

    OUTBOX_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Outbox 데이터 저장 실패"),
    OUTBOX_MAX_RETRY(HttpStatus.INTERNAL_SERVER_ERROR, "Outbox 재시도 횟수 최대치 도달");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
