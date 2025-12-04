package com.bidket.auction.domain.model.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuctionPeriod {

    @Column(nullable = false, name = "start_time")
    private LocalDateTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false, name = "original_end_time")
    private LocalDateTime originalEndTime;

    @Column(nullable = false, name = "extension_count")
    private Integer extensionCount;

    private static final int MAX_EXTENSIONS = 3;
    private static final int EXTENSION_MINUTES = 5;

    public static AuctionPeriod createDefault(LocalDateTime startTime, LocalDateTime endTime) {
        return AuctionPeriod.builder()
                .startTime(startTime)
                .endTime(endTime)
                .originalEndTime(endTime)
                .extensionCount(0)
                .build();
    }

    public void validate() {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시작시간과 종료시간은 필수입니다");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("종료시간은 시작시간보다 이후여야 합니다");
        }
    }

    public AuctionPeriod extend() {
        if (this.extensionCount >= MAX_EXTENSIONS) {
            throw new IllegalStateException("최대 연장 횟수를 초과했습니다");
        }
        return AuctionPeriod.builder()
                .startTime(this.startTime)
                .endTime(this.endTime.plusMinutes(EXTENSION_MINUTES))
                .originalEndTime(this.originalEndTime)
                .extensionCount(this.extensionCount + 1)
                .build();
    }

    public AuctionPeriod withUpdatedTimes(LocalDateTime startTime, LocalDateTime endTime) {
        return AuctionPeriod.builder()
                .startTime(startTime != null ? startTime : this.startTime)
                .endTime(endTime != null ? endTime : this.endTime)
                .originalEndTime(endTime != null ? endTime : this.originalEndTime)
                .extensionCount(this.extensionCount)
                .build();
    }

    public AuctionPeriod withReopenedEndTime(LocalDateTime newEndTime) {
        return AuctionPeriod.builder()
                .startTime(this.startTime)
                .endTime(newEndTime)
                .originalEndTime(this.originalEndTime)
                .extensionCount(this.extensionCount)
                .build();
    }

    public boolean isNearEnd() {
        return LocalDateTime.now().isAfter(this.endTime.minusMinutes(5));
    }

    public boolean isEnded() {
        return LocalDateTime.now().isAfter(this.endTime);
    }
}

