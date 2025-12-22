package com.bidket.user.presentation.dto.request;

import com.bidket.user.presentation.dto.request.validation.AtLeastOneField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 DTO
 * 최소 1개 필드는 포함되어야 함 (nickname / phone / email 중 택1 이상)
 */
@AtLeastOneField
@Schema(description = "프로필 수정 요청")
public record ProfileUpdateRequest(
        /** 변경할 닉네임 */
        @Size(max = 100, message = "닉네임은 100자 이하여야 합니다.")
        @Schema(description = "변경할 닉네임", example = "콘서트대장")
        String nickname,

        /** 변경할 전화번호 (하이픈, 공백 포함 가능, 서버에서 하이픈과 공백 제거 후 저장) */
        @Pattern(regexp = "^[0-9]+([-\\s][0-9]+)*$", message = "전화번호는 숫자로 시작해야 하며, 하이픈, 공백, 숫자 조합만 허용됩니다.")
        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        @Schema(description = "변경할 전화번호 (하이픈, 공백 포함 가능, 서버에서 하이픈과 공백 제거 후 저장)", example = "01012345678")
        String phone,

        /** 변경할 이메일 */
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        @Schema(description = "변경할 이메일", example = "newmail@example.com")
        String email
) {
    /**
     * 최소 1개 필드가 포함되어야 함
     */
    public boolean hasAtLeastOneField() {
        return (nickname != null && !nickname.isBlank()) ||
               (phone != null && !phone.isBlank()) ||
               (email != null && !email.isBlank());
    }
}

