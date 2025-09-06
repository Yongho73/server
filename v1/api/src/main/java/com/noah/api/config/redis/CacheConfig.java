package com.noah.api.config.redis;

import java.time.Duration;
import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching // @Cacheable, @CacheEvict 같은 애노테이션 사용 가능하게 함
public class CacheConfig {

    /**
     * RedisCacheManager Bean 정의
     * - 기본 TTL 10분
     * - null 값 캐싱 안 함
     * - Key: String 직렬화
     * - Value: JSON 직렬화
     * - 캐시별 TTL 개별 설정 가능
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        
        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 TTL 10분
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 캐시별 개별 TTL 설정 (필요한 경우 추가)
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "persons:list", defaultConfig.entryTtl(Duration.ofMinutes(3)) // persons:list 캐시는 3분
                // "products"  , defaultConfig.entryTtl(Duration.ofMinutes(5))
                // "users"     , defaultConfig.entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs) // 개별 TTL 적용
                .transactionAware()
                .build();
    }
}
