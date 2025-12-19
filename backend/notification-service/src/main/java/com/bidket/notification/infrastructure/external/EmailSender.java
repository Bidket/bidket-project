package com.bidket.notification.infrastructure.external;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SMTP를 통한 이메일 발송 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.properties.mail.from-name:Bidket}")
    private String fromName;

    /**
     * 이메일 발송 (단일 수신자, 텍스트 형식)
     * @param to 수신자 이메일 주소
     * @param subject 이메일 제목
     * @param text 이메일 내용 (텍스트)
     * @return 발송 성공 여부
     */
    public boolean sendEmail(String to, String subject, String text) {
        return sendEmail(to, subject, text, false);
    }

    /**
     * 이메일 발송 (단일 수신자)
     * @param to 수신자 이메일 주소
     * @param subject 이메일 제목
     * @param content 이메일 내용
     * @param isHtml HTML 형식 여부
     * @return 발송 성공 여부
     */
    public boolean sendEmail(String to, String subject, String content, boolean isHtml) {
        if (to == null || to.trim().isEmpty()) {
            log.warn("수신자 이메일 주소가 비어있습니다.");
            return false;
        }

        if (!isValidEmail(to)) {
            log.warn("유효하지 않은 이메일 주소 형식입니다: {}", to);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            mailSender.send(message);
            log.info("이메일 발송 성공: to={}, subject={}", to, subject);
            return true;

        } catch (MessagingException e) {
            log.error("이메일 발송 중 메시징 오류 발생: to={}, subject={}, error={}", 
                    to, subject, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("이메일 발송 중 예상치 못한 오류 발생: to={}, subject={}, error={}", 
                    to, subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 이메일 발송 (다중 수신자)
     * @param toList 수신자 이메일 주소 목록
     * @param subject 이메일 제목
     * @param content 이메일 내용
     * @param isHtml HTML 형식 여부
     * @return 발송 성공 여부
     */
    public boolean sendEmail(List<String> toList, String subject, String content, boolean isHtml) {
        if (toList == null || toList.isEmpty()) {
            log.warn("수신자 이메일 주소 목록이 비어있습니다.");
            return false;
        }

        // 유효한 이메일 주소만 필터링
        List<String> validEmails = toList.stream()
                .filter(email -> email != null && !email.trim().isEmpty() && isValidEmail(email))
                .toList();

        if (validEmails.isEmpty()) {
            log.warn("유효한 이메일 주소가 없습니다.");
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(validEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            mailSender.send(message);
            log.info("이메일 발송 성공: recipients={}, subject={}", validEmails.size(), subject);
            return true;

        } catch (MessagingException e) {
            log.error("이메일 발송 중 메시징 오류 발생: recipients={}, subject={}, error={}", 
                    validEmails.size(), subject, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("이메일 발송 중 예상치 못한 오류 발생: recipients={}, subject={}, error={}", 
                    validEmails.size(), subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * HTML 형식의 이메일 발송 (링크 포함)
     * @param to 수신자 이메일 주소
     * @param subject 이메일 제목
     * @param title 이메일 제목 (본문 내)
     * @param message 이메일 메시지
     * @param linkUrl 링크 URL (선택사항)
     * @return 발송 성공 여부
     */
    public boolean sendHtmlEmail(String to, String subject, String title, String message, String linkUrl) {
        String htmlContent = buildHtmlContent(title, message, linkUrl);
        return sendEmail(to, subject, htmlContent, true);
    }

    /**
     * HTML 이메일 내용 생성
     */
    private String buildHtmlContent(String title, String message, String linkUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; ");
        html.append("line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".container { background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 30px; }");
        html.append(".header { border-bottom: 2px solid #4CAF50; padding-bottom: 15px; margin-bottom: 20px; }");
        html.append(".title { font-size: 24px; font-weight: bold; color: #333; margin: 0; }");
        html.append(".content { font-size: 16px; color: #666; margin: 20px 0; }");
        html.append(".button { display: inline-block; padding: 12px 24px; background-color: #4CAF50; ");
        html.append("color: #ffffff; text-decoration: none; border-radius: 4px; margin-top: 20px; }");
        html.append(".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #e0e0e0; ");
        html.append("font-size: 12px; color: #999; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1 class='title'>").append(escapeHtml(title)).append("</h1>");
        html.append("</div>");
        html.append("<div class='content'>");
        html.append("<p>").append(escapeHtml(message).replace("\n", "<br>")).append("</p>");
        if (linkUrl != null && !linkUrl.trim().isEmpty()) {
            html.append("<a href='").append(escapeHtml(linkUrl)).append("' class='button'>자세히 보기</a>");
        }
        html.append("</div>");
        html.append("<div class='footer'>");
        html.append("<p>이 메일은 Bidket에서 발송되었습니다.</p>");
        html.append("<p>문의사항이 있으시면 고객센터로 연락해주세요.</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * HTML 특수문자 이스케이프
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 이메일 주소 유효성 검증 (간단한 형식 검증)
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // 간단한 이메일 형식 검증
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
