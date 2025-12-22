package com.bidket.product.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import com.bidket.product.domain.model.BrandStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "p_brand",
        indexes = {
                @Index(name = "idx_brand_name", columnList = "name"),
                @Index(name = "idx_brand_nameKr", columnList = "nameKr"),
                @Index(name = "idx_brand_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_kr", length = 100)
    private String nameKr;

    @Column(name = "origin_country", length = 50)
    private String originCountry;

    @Column(name = "website_url", length = 200)
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BrandStatus status;

    public static Brand of(String name, String nameKr, String originCountry, String websiteUrl) {
        Brand brand = new Brand();
        brand.name = name;
        brand.nameKr = nameKr;
        brand.originCountry = originCountry;
        brand.websiteUrl = websiteUrl;
        brand.status = BrandStatus.ACTIVE;
        return brand;
    }

    public void updateInfo(String name, String nameKr, String originCountry, String websiteUrl) {
        if (name != null) {
            this.name = name;
        }
        if (nameKr != null) {
            this.nameKr = nameKr;
        }
        if (originCountry != null) {
            this.originCountry = originCountry;
        }
        if (websiteUrl != null) {
            this.websiteUrl = websiteUrl;
        }
    }

    public void changeStatus(BrandStatus status) {
        this.status = status;
    }
}
