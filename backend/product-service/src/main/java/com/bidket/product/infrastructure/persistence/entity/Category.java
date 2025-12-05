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
        name = "p_category",
        indexes = {
                @Index(name = "idx_category_product_type", columnList = "product_type_id"),
                @Index(name = "idx_category_parent", columnList = "parent_id"),
                @Index(name = "idx_category_depth", columnList = "depth"),
                @Index(name = "idx_category_name", columnList = "name"),
                @Index(name = "idx_category_sort_id", columnList = "sort_id"),
                @Index(name = "idx_category_is_leaf", columnList = "is_leaf")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = false)
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parentId;

    @Column(nullable = false)
    private Integer depth;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, name = "sort_id")
    private Long sortId;

    @Column(nullable = false, name = "is_leaf")
    private Boolean isLeaf;
}
