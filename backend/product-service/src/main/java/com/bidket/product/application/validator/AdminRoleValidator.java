package com.bidket.product.application.validator;

import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import org.springframework.stereotype.Component;

@Component
public class AdminRoleValidator {

    public void validate(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ProductException(ProductErrorCode.FORBIDDEN);
        }
    }
}
