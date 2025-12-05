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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "p_size",
        indexes = {
                @Index(name = "idx_size_size_type", columnList = "size_type_id"),
                @Index(name = "idx_size_sort_id", columnList = "sort_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Size extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_type_id", nullable = false)
    private SizeType sizeType;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, name = "display_label", length = 50)
    private String displayLabel;

    @Column(nullable = false, name = "sort_id")
    private Long sortId;
}
