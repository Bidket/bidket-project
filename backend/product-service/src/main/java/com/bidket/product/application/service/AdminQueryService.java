package com.bidket.product.application.service;

import com.bidket.common.presentation.response.PageResponse;
import com.bidket.product.application.mapper.AdminProductMapper;
import com.bidket.product.application.mapper.AdminSizeMapper;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import com.bidket.product.domain.model.ProductStatus;
import com.bidket.product.infrastructure.persistence.entity.Product;
import com.bidket.product.infrastructure.persistence.entity.Size;
import com.bidket.product.infrastructure.persistence.entity.SizeType;
import com.bidket.product.infrastructure.persistence.repository.ProductAdminSpecification;
import com.bidket.product.infrastructure.persistence.repository.ProductRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeRepository;
import com.bidket.product.infrastructure.persistence.repository.SizeTypeRepository;
import com.bidket.product.presentation.dto.request.PageRequestDto;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminResponse;
import com.bidket.product.presentation.dto.response.product.ProductGetAdminSimpleResponse;
import com.bidket.product.presentation.dto.response.size.SizeGetAdminResponse;
import com.bidket.product.presentation.dto.response.size.SizeTypeGetAdminResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQueryService {

    private final SizeTypeRepository sizeTypeRepository;
    private final SizeRepository sizeRepository;
    private final ProductRepository productRepository;

    private final AdminSizeMapper adminSizeMapper;
    private final AdminProductMapper adminProductMapper;

    /** 사이즈 타입 목록 조회 */
    public PageResponse<SizeTypeGetAdminResponse> getSizeTypes(
            UUID productTypeId,
            PageRequestDto pageRequest
    ) {
        Pageable pageable = pageRequest.toPageable();

        Page<SizeType> page = (productTypeId == null)
                ? sizeTypeRepository.findAll(pageable)
                : sizeTypeRepository.findAllByProductType_Id(productTypeId, pageable);

        List<SizeTypeGetAdminResponse> resList = page.getContent().stream()
                .map(adminSizeMapper::toSizeTypeDto)
                .toList();

        return PageResponse.of(
                resList,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    /** 사이즈 목록 조회 */
    public PageResponse<SizeGetAdminResponse> getSizes(
            UUID sizeTypeId,
            PageRequestDto pageRequest
    ) {
        // 사이즈의 정렬용 인덱스 sortId 기준으로 정렬
        Pageable pageable = pageRequest.toPageable(
                Sort.by(Direction.ASC, "sortId")
        );

        Page<Size> page =  (sizeTypeId == null)
                ? sizeRepository.findAll(pageable)
                : sizeRepository.findAllBySizeType_Id(sizeTypeId, pageable);

        List<SizeGetAdminResponse> resList = page.getContent().stream()
                .map(adminSizeMapper::toSizeDto)
                .toList();

        return PageResponse.of(
                resList,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    /** 상품 목록 조회 */
    public PageResponse<ProductGetAdminResponse> getProducts(
            ProductStatus status,
            UUID brandId,
            UUID productTypeId,
            PageRequestDto pageRequest
    ) {
        Pageable pageable = pageRequest.toPageable(Sort.by(Direction.DESC, "createdAt")
        );

        Specification<Product> spec = ProductAdminSpecification.hasStatus(status)
                .and(ProductAdminSpecification.hasBrand(brandId))
                .and(ProductAdminSpecification.hasProductType(productTypeId));

        Page<Product> page = productRepository.findAll(spec, pageable);

        List<ProductGetAdminResponse> resList = page.getContent().stream()
                .map(adminProductMapper::toListDto)
                .toList();

        return PageResponse.of(
                resList,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    /** 상품 단건 단순 조회 */
    public ProductGetAdminSimpleResponse getProduct(
            UUID productId
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductException(ProductErrorCode.PRODUCT_NOT_FOUND));

        return adminProductMapper.toSimpleDto(product);
    }
}
