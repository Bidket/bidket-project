package com.bidket.product.application.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bidket.product.common.TestFixture;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.infrastructure.persistence.entity.ProductType;
import com.bidket.product.infrastructure.persistence.entity.SizeType;
import com.bidket.product.presentation.dto.request.size.SizeCreateRequest;
import com.bidket.product.presentation.dto.request.size.SizeTypeCreateRequest;
import com.bidket.product.presentation.dto.response.size.SizeCreateResponse;
import com.bidket.product.presentation.dto.response.size.SizeTypeCreateResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("local")
public class SizeServiceIntegrationTest {

    @Autowired
    SizeService sizeService;

    @Autowired
    TestFixture fixture;

    ProductType pt;
    SizeType st;

    @BeforeEach
    void setup() {
        pt = fixture.createProductType();
        st = fixture.createSizeType(pt);
    }

    @Test
    void createSizeType_success() {
        SizeTypeCreateRequest req = new SizeTypeCreateRequest(
                pt.getId(),
                "SHOES_KR_MM",
                "KR",
                "사이즈타입 KR 설명",
                true
        );

        SizeTypeCreateResponse res = sizeService.createSizeType(req);
        assertNotNull(res);
    }

    @Test
    void createSize_success() {
        SizeCreateRequest req = new SizeCreateRequest(
                st.getId(),
                "260",
                "260mm",
                100L
        );

        SizeCreateResponse res = sizeService.createSize(req);
        assertNotNull(res);
    }

    @Test
    void createSize_fail_whenSizeTypeNotExists() {

        SizeCreateRequest req = new SizeCreateRequest(
                UUID.randomUUID(), "US 10", "US 10", 100L
        );

        assertThrows(ProductException.class,
                () -> sizeService.createSize(req));
    }
}
