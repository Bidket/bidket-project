package com.bidket.queue.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public class QueueConfigModel {
    private UUID auctionId;
    private Long maxActiveUsers;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;


}
