-- ============================================================================
-- NotificationChannel 체크 제약 조건 업데이트
-- 날짜: 2025-12-12
-- 변경 사항: SYSTEM → IN_APP
-- ============================================================================

-- 1. 기존 제약 조건 삭제
ALTER TABLE p_notification DROP CONSTRAINT IF EXISTS p_notification_channel_check;

-- 2. 기존 SYSTEM 값을 IN_APP으로 변경 (데이터 마이그레이션)
UPDATE p_notification SET channel = 'IN_APP' WHERE channel = 'SYSTEM';

-- 3. 새로운 제약 조건 추가 (IN_APP 포함)
ALTER TABLE p_notification 
ADD CONSTRAINT p_notification_channel_check 
CHECK (channel IN ('PUSH', 'EMAIL', 'SMS', 'SLACK', 'IN_APP'));

