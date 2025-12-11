package com.bidket.product.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.product.domain.model.ProductDetail;
import com.bidket.product.domain.model.Silhouette;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "p_product_shoes_detail",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_shoes_detail_product", columnNames = "product_id")
        },
        indexes = {
                @Index(name = "idx_shoes_detail_product", columnList = "product_id"),
                @Index(name = "idx_shoes_detail_colorway", columnList = "colorway"),
                @Index(name = "idx_shoes_detail_mainMaterial", columnList = "mainMaterial"),
                @Index(name = "idx_shoes_detail_silhouette", columnList = "silhouette")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductShoesDetail extends BaseEntity implements ProductDetail {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String colorway;

    @Column(nullable = false, name = "main_material", length = 50)
    private String mainMaterial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Silhouette silhouette;

    @Column(length = 100)
    private String style;

    @Column(name = "origin_country", length = 50)
    private String originCountry;

    private BigDecimal weight;

    @Override
    public UUID getProductId() {
        return product != null ? product.getId() : null;
    }

    public static ProductShoesDetail create(
            Product product,
            String colorway,
            String mainMaterial,
            Silhouette silhouette,
            String style,
            String originCountry,
            BigDecimal weight
    ) {
        ProductShoesDetail detail = new ProductShoesDetail();
        detail.product = product;
        detail.colorway = colorway;
        detail.mainMaterial = mainMaterial;
        detail.silhouette = silhouette;
        detail.style = style;
        detail.originCountry = originCountry;
        detail.weight = weight;
        return detail;
    }
}
