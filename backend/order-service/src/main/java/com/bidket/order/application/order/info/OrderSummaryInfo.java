package com.bidket.order.application.order.info;

import com.bidket.order.domain.order.model.Order;
import com.bidket.order.domain.order.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;

@Getter
public class OrderSummaryInfo {

    private final UUID orderId;
    private final OrderStatus status;
    private final Long amount;
    private final String productName;           // TODO: 상품/경매 연동
    private final String auctionTitle;
    private final LocalDateTime auctionStartTime;
    private final LocalDateTime createdAt;

    public OrderSummaryInfo(
            UUID orderId,
            OrderStatus status,
            Long amount,
            String productName,
            String auctionTitle,
            LocalDateTime auctionStartTime,
            LocalDateTime createdAt
    ) {
        this.orderId = orderId;
        this.status = status;
        this.amount = amount;
        this.productName = productName;
        this.auctionTitle = auctionTitle;
        this.auctionStartTime = auctionStartTime;
        this.createdAt = createdAt;
    }

    public static OrderSummaryInfo from(Order order) {
        return new OrderSummaryInfo(
                order.id(),
                order.status(),
                order.amount(),
                null, // TODO: 신발/경매 서비스 연동
                null, // TODO: 경매 제목
                null, // TODO: 경매 시작 시간
                order.createdAt()
        );
    }
}