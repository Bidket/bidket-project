package com.bidket.product.domain.model;

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
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "p_product_category",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_category_product_category",
                        columnNames = {"product_id", "category_id"}
                )
        },
        indexes = {
                @Index(name = "idx_product_category_category", columnList = "category_id"),
                @Index(name = "idx_product_category_product", columnList = "product_id"),
                @Index(name = "idx_product_category_is_primary", columnList = "is_primary")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCategory extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, name = "is_primary")
    private Boolean isPrimary;
}
