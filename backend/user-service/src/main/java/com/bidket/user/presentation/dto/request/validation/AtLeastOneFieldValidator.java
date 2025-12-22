package com.bidket.user.presentation.dto.request.validation;

import com.bidket.user.presentation.dto.request.ProfileUpdateRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/*
 * 최소 1개 필드 포함 검증 Validator
 */
public class AtLeastOneFieldValidator implements ConstraintValidator<AtLeastOneField, ProfileUpdateRequest> {

    @Override
    public void initialize(AtLeastOneField constraintAnnotation) {
        // 초기화 로직이 필요한 경우 여기에 작성
    }

    @Override
    public boolean isValid(ProfileUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }

        // ProfileUpdateRequest의 hasAtLeastOneField() 메서드를 직접 호출
        boolean isValid = request.hasAtLeastOneField();

        if (!isValid) {
            // 커스텀 에러 메시지 설정
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("최소 1개 필드는 포함되어야 합니다. (nickname, phone, email 중 택1 이상)")
                    .addConstraintViolation();
        }

        return isValid;
    }
}

