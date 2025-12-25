package com.bidket.auction.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS, 
                JsonTypeInfo.As.PROPERTY
        );

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(objectMapper)));

        // 입찰 관련 캐시: 짧은 TTL (1분) - 자주 변경됨
        RedisCacheConfiguration highestBidConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(1));

        // 입찰 목록 캐시: 중간 TTL (3분)
        RedisCacheConfiguration bidsConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(3));

        // 경매 캐시: 기본 TTL (10분)
        RedisCacheConfiguration auctionConfig = defaultConfig
                .entryTtl(Duration.ofMinutes(10));

        // 경매 목록 캐시: 짧은 TTL (30초) - 목록은 자주 변경됨
        RedisCacheConfiguration auctionsByStatusConfig = defaultConfig
                .entryTtl(Duration.ofSeconds(30));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("highestBid", highestBidConfig)
                .withCacheConfiguration("bids", bidsConfig)
                .withCacheConfiguration("auctions", auctionConfig)
                .withCacheConfiguration("auctionsByStatus", auctionsByStatusConfig)
                .build();
    }
}
