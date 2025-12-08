package com.bidket.product.domain.model;

import com.bidket.common.infra.BaseEntity;
import com.bidket.product.domain.model.enums.Gender;
import com.bidket.product.domain.model.enums.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "p_product",
        indexes = {
                @Index(name = "idx_product_product_type", columnList = "product_type_id"),
                @Index(name = "idx_product_brand", columnList = "brand_id"),
                @Index(name = "idx_product_name", columnList = "name"),
                @Index(name = "idx_product_nameKr", columnList = "nameKr"),
                @Index(name = "idx_product_release_date", columnList = "release_date"),
                @Index(name = "idx_product_release_price", columnList = "release_price"),
                @Index(name = "idx_product_status", columnList = "status"),
                @Index(
                        name = "idx_product_type_status_date",
                        columnList = "product_type_id, status, release_date"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = false)
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String nameKr;

    @Column(name = "model_code", length = 100)
    private String modelCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(nullable = false, name = "release_price", precision = 12, scale = 2)
    private BigDecimal releasePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;
}
