package com.bidket.user.infrastructure.persistence.entity;

import com.bidket.common.infra.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_role", uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_code", columnNames = "code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", length = 50)
    private String name;

    @Builder
    public Role(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public void updateName(String name) {
        this.name = name;
    }
}

