package com.bidket.user.infrastructure.external;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Google idToken 검증 클래스
 * Google의 JWKS 엔드포인트에서 공개 키를 가져와서 idToken을 검증합니다.
 * 검증 항목:
 * - 서명 검증 (JWT 서명)
 * - iss (issuer) 검증: https://accounts.google.com 또는 accounts.google.com
 * - aud (audience) 검증: 클라이언트 ID와 일치
 * - exp (expiration) 검증: 토큰 만료 시간
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private static final String GOOGLE_JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final String GOOGLE_ISSUER_HTTPS = "https://accounts.google.com";
    private static final String GOOGLE_ISSUER_PLAIN = "accounts.google.com";
    
    private final RestClient restClient;

    @Value("${google.client-id}")
    private String clientId;

    /**
     * Google idToken 검증 및 사용자 정보 추출
     * @param idToken Google idToken
     * @return 검증된 사용자 정보 (providerId, email, name)
     * @throws UserException 토큰 검증 실패 시
     */
    public GoogleUserInfo verifyToken(String idToken) {
        try {
            // 1. 토큰에서 kid 추출
            String kid = getKidFromToken(idToken);
            
            // 2. JWKS에서 공개 키 가져오기
            PublicKey publicKey = getPublicKeyFromJWKS(kid);
            
            // 3. 토큰 검증 및 파싱 (서명 검증)
            Claims verifiedClaims;
            
            try {
                verifiedClaims = Jwts.parser()
                        .verifyWith(publicKey)
                        .build()
                        .parseSignedClaims(idToken)
                        .getPayload();
            } catch (io.jsonwebtoken.security.SignatureException e) {
                log.warn("Google idToken 서명 검증 실패: {}", e.getMessage());
                throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.warn("Google idToken 만료됨");
                throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
            } catch (io.jsonwebtoken.JwtException e) {
                log.warn("Google idToken 검증 실패: {}", e.getMessage());
                throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
            }

            // 4. iss (issuer) 검증 (두 가지 형태 모두 허용)
            String iss = verifiedClaims.getIssuer();
            if (!GOOGLE_ISSUER_HTTPS.equals(iss) && !GOOGLE_ISSUER_PLAIN.equals(iss)) {
                log.warn("Google idToken issuer 불일치: expected={} or {}, actual={}", 
                        GOOGLE_ISSUER_HTTPS, GOOGLE_ISSUER_PLAIN, iss);
                throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
            }

            // 5. aud 검증
            Object audObj = verifiedClaims.get("aud");
            boolean audValid = false;
            
            if (audObj == null) {
                log.warn("Google idToken audience가 null임");
                throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
            }
            
            if (audObj instanceof String) {
                audValid = clientId.equals(audObj);
            } else if (audObj instanceof Collection) {
                // List, Set 등 Collection 타입 처리
                Collection<?> audCollection = (Collection<?>) audObj;
                
                // Collection의 각 요소를 String으로 변환하여 비교
                for (Object item : audCollection) {
                    String audItem = null;
                    if (item instanceof String) {
                        audItem = (String) item;
                    } else if (item != null) {
                        audItem = item.toString().trim();
                    }
                    
                    if (audItem == null) {
                        continue;
                    }
                    
                    // 정확한 문자열 비교 (trim 적용)
                    if (clientId.equals(audItem.trim())) {
                        audValid = true;
                        break;
                    }
                }
            } else {
                // 기타 타입 (예: Number 등)
                String audStr = audObj.toString();
                audValid = clientId.equals(audStr);
                log.warn("Google idToken audience 검증 (기타 타입): expected={}, actual={}, actualType={}, valid={}", 
                        clientId, audStr, audObj.getClass().getName(), audValid);
            }
            
            if (!audValid) {
                log.warn("Google idToken audience 불일치: expected='{}' (length={}), actual={}, actualType={}", 
                        clientId, clientId != null ? clientId.length() : 0, 
                        audObj, audObj != null ? audObj.getClass().getName() : "null");
                throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
            }

            // 6. 사용자 정보 추출
            String providerId = verifiedClaims.getSubject(); // sub 클레임
            String email = verifiedClaims.get("email", String.class);
            String name = verifiedClaims.get("name", String.class);

            return new GoogleUserInfo(providerId, email, name);

        } catch (UserException e) {
            throw e; // UserException은 그대로 던짐 (이미 적절한 에러 코드로 설정됨)
        } catch (Exception e) {
            log.error("Google idToken 검증 실패: {}", e.getMessage(), e);
            throw new UserException(UserErrorCode.SERVER_ERROR);
        }
    }

    /**
     * 토큰에서 kid (Key ID) 추출
     */
    private String getKidFromToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
            }
            
            String header = new String(Base64.getUrlDecoder().decode(parts[0]));
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> headerMap = objectMapper.readValue(header, Map.class);
            
            String kid = (String) headerMap.get("kid");
            if (kid == null || kid.isBlank()) {
                log.warn("토큰 헤더에 kid가 없거나 비어있음");
                throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
            }
            
            return kid;
        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            log.error("토큰 헤더에서 kid 추출 실패: {}", e.getMessage());
            throw new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
        }
    }

    /**
     * Google JWKS 엔드포인트에서 공개 키 가져오기
     */
    private PublicKey getPublicKeyFromJWKS(String kid) {
        try {
            log.debug("Google JWKS 엔드포인트 호출: {}", GOOGLE_JWKS_URL);
            
            GoogleJWKSResponse jwksResponse = restClient.get()
                    .uri(GOOGLE_JWKS_URL)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, response1) -> {
                        // JWKS 엔드포인트 호출 실패는 Google 서버 문제이므로 SERVER_ERROR
                        log.error("Google JWKS 엔드포인트 호출 실패: status={}, url={}", 
                                response1.getStatusCode().value(), GOOGLE_JWKS_URL);
                        throw new UserException(UserErrorCode.SERVER_ERROR);
                    })
                    .body(GoogleJWKSResponse.class);

            if (jwksResponse == null || jwksResponse.getKeys() == null) {
                log.error("Google JWKS 응답이 null이거나 keys가 없음");
                throw new UserException(UserErrorCode.SERVER_ERROR);
            }

            // kid에 해당하는 키 찾기
            GoogleJWKSResponse.Key key = jwksResponse.getKeys().stream()
                    .filter(k -> kid.equals(k.getKid()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.warn("JWKS에서 kid에 해당하는 키를 찾을 수 없음: kid={}", kid);
                        return new UserException(UserErrorCode.SOCIAL_TOKEN_INVALID);
                    });

            // RSA 공개 키 생성
            byte[] modulusBytes = Base64.getUrlDecoder().decode(key.getN());
            byte[] exponentBytes = Base64.getUrlDecoder().decode(key.getE());

            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);

        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            log.error("공개 키 생성 실패: {}", e.getMessage(), e);
            throw new UserException(UserErrorCode.SERVER_ERROR);
        }
    }

    /**
     * Google 사용자 정보
     */
    @Data
    public static class GoogleUserInfo {
        private final String providerId; // sub 클레임
        private final String email;
        private final String name;
    }

    /**
     * Google JWKS 응답 DTO
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleJWKSResponse {
        @JsonProperty("keys")
        private List<Key> keys;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Key {
            @JsonProperty("kty")
            private String kty;

            @JsonProperty("kid")
            private String kid;

            @JsonProperty("use")
            private String use;

            @JsonProperty("n")
            private String n;

            @JsonProperty("e")
            private String e;

            @JsonProperty("alg")
            private String alg;
        }
    }
}

