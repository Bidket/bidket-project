package com.bidket.order.infrastructure.order.client;

import com.bidket.order.application.order.port.AuctionQueryPort;
import com.bidket.order.application.order.port.AuctionSnapshot;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AuctionRestTemplateAdapter implements AuctionQueryPort {

    private final RestTemplate restTemplate;

    @Value("${services.auction.base-url}")
    private String auctionBaseUrl;

    @Override
    public AuctionSnapshot getAuction(UUID auctionId) {
        String url = auctionBaseUrl + "/api/v1/auctions/{auctionId}";

        try {
            AuctionClientResponse res =
                    restTemplate.getForObject(url, AuctionClientResponse.class, auctionId);

            if (res == null) {
                throw new IllegalStateException("auction response is null");
            }

            return new AuctionSnapshot(
                    res.auctionId(),
                    res.productSizeId(),
                    res.sellerId(),
                    res.auctionTitle(),
                    res.startTime(),
                    res.status(),
                    res.winnerId(),
                    res.finalPrice()
            );
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(
                    "auction-service 호출 실패. status=" + e.getStatusCode(),
                    e
            );
        } catch (RestClientException e) {
            throw new IllegalStateException("auction-service 호출 실패", e);
        }
    }
}