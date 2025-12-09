package com.bidket.notification.infrastructure.external;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Slack Webhook을 통한 메시지 발송 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackSender {

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    private final RestTemplate restTemplate;

    /**
     * Slack으로 메시지 발송
     * @param title 메시지 제목
     * @param message 메시지 내용
     * @param linkUrl 링크 URL (선택사항)
     * @return 발송 성공 여부
     */
    public boolean sendMessage(String title, String message, String linkUrl) {
        try {
            SlackMessage slackMessage = SlackMessage.builder()
                    .text(title)
                    .blocks(createBlocks(title, message, linkUrl))
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SlackMessage> request = new HttpEntity<>(slackMessage, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    webhookUrl,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Slack 메시지 발송 성공: {}", title);
                return true;
            } else {
                log.warn("Slack 메시지 발송 실패: status={}, body={}", 
                        response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Slack 메시지 발송 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Slack Block Kit 형식으로 메시지 블록 생성
     */
    private Object[] createBlocks(String title, String message, String linkUrl) {
        Block headerBlock = Block.builder()
                .type("header")
                .text(BlockText.builder()
                        .type("plain_text")
                        .text(title)
                        .build())
                .build();

        Block messageBlock = Block.builder()
                .type("section")
                .text(BlockText.builder()
                        .type("mrkdwn")
                        .text(message)
                        .build())
                .build();

        if (linkUrl != null && !linkUrl.isEmpty()) {
            Block linkBlock = Block.builder()
                    .type("section")
                    .text(BlockText.builder()
                            .type("mrkdwn")
                            .text(String.format("<%s|자세히 보기>", linkUrl))
                            .build())
                    .build();
            return new Object[]{headerBlock, messageBlock, linkBlock};
        }

        return new Object[]{headerBlock, messageBlock};
    }

    /**
     * Slack 메시지 DTO
     */
    @Data
    @Builder
    private static class SlackMessage {
        private String text;
        private Object[] blocks;
    }

    /**
     * Slack Block DTO
     */
    @Data
    @Builder
    private static class Block {
        private String type;
        private BlockText text;
    }

    /**
     * Slack Block Text DTO
     */
    @Data
    @Builder
    private static class BlockText {
        private String type;
        private String text;
    }
}

