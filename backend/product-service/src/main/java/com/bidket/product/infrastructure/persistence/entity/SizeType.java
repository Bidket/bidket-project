package com.bidket.product.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "p_size_type",
        indexes = {
                @Index(name = "idx_size_type_product_type", columnList = "product_type_id"),
                @Index(name = "idx_size_type_code", columnList = "code")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SizeType extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = false)
    private ProductType productType;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "is_default")
    private Boolean isDefault;

    public static SizeType create(
            ProductType productType,
            String code,
            String regionCode,
            String description,
            Boolean isDefault
    ) {
        SizeType type = new SizeType();
        type.productType = productType;
        type.code = code;
        type.regionCode = regionCode;
        type.description = description;
        type.isDefault = isDefault;
        return type;
    }
}
