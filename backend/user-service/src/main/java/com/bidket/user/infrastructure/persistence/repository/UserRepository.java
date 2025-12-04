package com.bidket.user.infrastructure.persistence.repository;

import com.bidket.user.infrastructure.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /** 이메일로 사용자 조회 */
    Optional<User> findByEmail(String email);
    
    /** 로그인 아이디로 사용자 조회 */
    Optional<User> findByLoginId(String loginId);
    
    /** 이메일 중복 확인 */
    boolean existsByEmail(String email);
    
    /** 로그인 아이디 중복 확인 */
    boolean existsByLoginId(String loginId);
    
    /** 닉네임 중복 확인 */
    boolean existsByNickname(String nickname);
}

