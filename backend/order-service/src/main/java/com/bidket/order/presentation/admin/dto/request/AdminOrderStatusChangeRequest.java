package com.bidket.order.presentation.admin.dto.request;

import com.bidket.order.domain.order.model.OrderStatus;

public record AdminOrderStatusChangeRequest(
        OrderStatus status,
        String reason
) {

}