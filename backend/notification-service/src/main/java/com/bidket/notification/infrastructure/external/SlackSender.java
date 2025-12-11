package com.bidket.notification.infrastructure.external;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Slack Webhook을 통한 메시지 발송 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackSender {

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    private final RestClient restClient;

    /**
     * Slack으로 메시지 발송 (기본 webhook URL 사용)
     * @param title 메시지 제목
     * @param message 메시지 내용
     * @param linkUrl 링크 URL (선택사항)
     * @return 발송 성공 여부
     */
    public boolean sendMessage(String title, String message, String linkUrl) {
        return sendMessage(title, message, linkUrl, null);
    }

    /**
     * Slack으로 메시지 발송 (동적 webhook URL 지원)
     * @param title 메시지 제목
     * @param message 메시지 내용
     * @param linkUrl 링크 URL (선택사항)
     * @param customWebhookUrl 커스텀 webhook URL (null이면 기본 webhook URL 사용)
     * @return 발송 성공 여부
     */
    public boolean sendMessage(String title, String message, String linkUrl, String customWebhookUrl) {
        try {
            String targetWebhookUrl = (customWebhookUrl != null && !customWebhookUrl.isEmpty()) 
                    ? customWebhookUrl 
                    : webhookUrl;

            if (targetWebhookUrl == null || targetWebhookUrl.isEmpty()) {
                log.warn("Slack webhook URL이 설정되지 않았습니다.");
                return false;
            }

            // webhook URL 형식 검증
            if (!targetWebhookUrl.startsWith("https://hooks.slack.com/services/")) {
                log.warn("유효하지 않은 Slack webhook URL 형식입니다: {}", targetWebhookUrl);
                return false;
            }

            // Slack 메시지 생성 (간단한 형식과 Block Kit 형식 모두 지원)
            SlackMessage slackMessage;
            if (linkUrl != null && !linkUrl.isEmpty()) {
                // Block Kit 형식 사용
                slackMessage = SlackMessage.builder()
                        .text(title) // fallback 텍스트
                        .blocks(createBlocks(title, message, linkUrl))
                        .build();
            } else {
                // 간단한 형식 사용 (더 안정적)
                String fullMessage = title + "\n" + message;
                slackMessage = SlackMessage.builder()
                        .text(fullMessage)
                        .build();
            }

            // 요청 로깅 (디버깅용)
            log.debug("Slack 메시지 발송 요청: URL={}, Title={}", targetWebhookUrl, title);

            // RestClient를 사용한 요청
            var response = restClient.post()
                    .uri(targetWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(slackMessage)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                            (request, response1) -> {
                                String errorBody = new String(response1.getBody().readAllBytes());
                                log.error("Slack API 오류 발생: status={}, body={}, URL={}", 
                                        response1.getStatusCode(), errorBody, targetWebhookUrl);
                                
                                if (response1.getStatusCode().value() == 404 && errorBody.contains("no_team")) {
                                    log.error("'no_team' 오류: Slack webhook URL이 유효하지 않거나 만료되었을 수 있습니다. " +
                                             "Slack 워크스페이스 설정을 확인하거나 새로운 webhook을 생성해주세요.");
                                }
                                
                                throw new org.springframework.web.client.HttpClientErrorException(
                                        response1.getStatusCode(), 
                                        errorBody
                                );
                            })
                    .toEntity(String.class);

            // 응답 처리
            String responseBody = response.getBody();
            log.debug("Slack 응답: status={}, body={}", response.getStatusCode(), responseBody);

            // Slack webhook은 성공 시 "ok" 문자열을 반환
            if (response.getStatusCode().is2xxSuccessful() && 
                responseBody != null && responseBody.equals("ok")) {
                log.info("Slack 메시지 발송 성공: {}", title);
                return true;
            } else {
                log.warn("Slack 메시지 발송 실패: status={}, body={}", 
                        response.getStatusCode(), responseBody);
                return false;
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 이미 onStatus에서 로깅했으므로 여기서는 false만 반환
            return false;
        } catch (org.springframework.web.client.RestClientException e) {
            log.error("Slack 메시지 발송 중 네트워크 오류 발생: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Slack 메시지 발송 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
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

