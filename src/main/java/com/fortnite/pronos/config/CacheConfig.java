package com.fortnite.pronos.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/** Cache configuration optimized for production and local development. */
@Configuration
@EnableCaching
public class CacheConfig {

  private static final long LEADERBOARD_TTL_MINUTES = 10L;
  private static final long DEFAULT_TTL_MINUTES = 30L;
  private static final long PLAYER_SCORES_TTL_MINUTES = 15L;
  private static final long GAMES_TTL_MINUTES = 60L;
  private static final long DASHBOARD_TTL_MINUTES = 20L;
  private static final long PLAYERS_TTL_HOURS = 24L;
  private static final long PLAYER_PAGES_TTL_HOURS = 12L;
  private static final long REGION_DISTRIBUTION_TTL_HOURS = 12L;
  private static final long TEAM_COMPOSITIONS_TTL_HOURS = 2L;
  private static final long CATALOGUE_TTL_HOURS = 24L;

  private static final String CACHE_LEADERBOARD = "leaderboard";
  private static final String CACHE_LEADERBOARD_STATS = "leaderboard-stats";
  private static final String CACHE_GAME_STATS = "gameStats";
  private static final String CACHE_REGION_DISTRIBUTION = "regionDistribution";
  private static final String CACHE_PLAYER_SCORES = "playerScores";
  private static final String CACHE_GAMES = "games";
  private static final String CACHE_PLAYERS = "players";
  private static final String CACHE_PLAYER_PAGES = "playerPages";
  private static final String CACHE_REGION_DISTRIBUTION_AGGREGATED = "region-distribution";
  private static final String CACHE_TEAM_COMPOSITIONS = "team-compositions";
  private static final String CACHE_SCORES = "scores";
  private static final String CACHE_DASHBOARD = "dashboard";
  private static final String CACHE_CATALOGUE_ALL = "catalogue-all";
  private static final String CACHE_CATALOGUE_REGION = "catalogue-region";

  private static final List<String> CACHE_NAMES =
      List.of(
          CACHE_LEADERBOARD,
          CACHE_LEADERBOARD_STATS,
          CACHE_GAME_STATS,
          CACHE_REGION_DISTRIBUTION,
          CACHE_PLAYER_SCORES,
          CACHE_GAMES,
          CACHE_PLAYERS,
          CACHE_PLAYER_PAGES,
          CACHE_REGION_DISTRIBUTION_AGGREGATED,
          CACHE_TEAM_COMPOSITIONS,
          CACHE_SCORES,
          CACHE_DASHBOARD,
          CACHE_CATALOGUE_ALL,
          CACHE_CATALOGUE_REGION);

  @Bean
  @Primary
  @ConditionalOnProperty(name = "spring.data.redis.host")
  public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
    RedisCacheConfiguration defaultConfig =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(DEFAULT_TTL_MINUTES))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
    cacheConfigurations.put(
        CACHE_LEADERBOARD, defaultConfig.entryTtl(Duration.ofMinutes(LEADERBOARD_TTL_MINUTES)));
    cacheConfigurations.put(
        CACHE_LEADERBOARD_STATS, defaultConfig.entryTtl(Duration.ofMinutes(DEFAULT_TTL_MINUTES)));
    cacheConfigurations.put(
        CACHE_GAME_STATS, defaultConfig.entryTtl(Duration.ofMinutes(DEFAULT_TTL_MINUTES)));
    cacheConfigurations.put(
        CACHE_REGION_DISTRIBUTION,
        defaultConfig.entryTtl(Duration.ofHours(REGION_DISTRIBUTION_TTL_HOURS)));
    cacheConfigurations.put(
        CACHE_PLAYER_SCORES, defaultConfig.entryTtl(Duration.ofMinutes(PLAYER_SCORES_TTL_MINUTES)));
    cacheConfigurations.put(
        CACHE_GAMES, defaultConfig.entryTtl(Duration.ofMinutes(GAMES_TTL_MINUTES)));
    cacheConfigurations.put(
        CACHE_PLAYERS, defaultConfig.entryTtl(Duration.ofHours(PLAYERS_TTL_HOURS)));
    cacheConfigurations.put(
        CACHE_PLAYER_PAGES, defaultConfig.entryTtl(Duration.ofHours(PLAYER_PAGES_TTL_HOURS)));
    cacheConfigurations.put(
        CACHE_REGION_DISTRIBUTION_AGGREGATED,
        defaultConfig.entryTtl(Duration.ofHours(REGION_DISTRIBUTION_TTL_HOURS)));
    cacheConfigurations.put(
        CACHE_TEAM_COMPOSITIONS,
        defaultConfig.entryTtl(Duration.ofHours(TEAM_COMPOSITIONS_TTL_HOURS)));
    cacheConfigurations.put(
        CACHE_SCORES, defaultConfig.entryTtl(Duration.ofMinutes(PLAYER_SCORES_TTL_MINUTES)));
    cacheConfigurations.put(
        CACHE_DASHBOARD, defaultConfig.entryTtl(Duration.ofMinutes(DASHBOARD_TTL_MINUTES)));
    cacheConfigurations.put(
        CACHE_CATALOGUE_ALL, defaultConfig.entryTtl(Duration.ofHours(CATALOGUE_TTL_HOURS)));
    cacheConfigurations.put(
        CACHE_CATALOGUE_REGION, defaultConfig.entryTtl(Duration.ofHours(CATALOGUE_TTL_HOURS)));

    return RedisCacheManager.builder(redisConnectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .transactionAware()
        .build();
  }

  @Bean
  @ConditionalOnProperty(
      name = "spring.data.redis.host",
      havingValue = "false",
      matchIfMissing = true)
  @Profile("!test")
  public CacheManager concurrentMapCacheManager() {
    ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
    cacheManager.setCacheNames(CACHE_NAMES);
    return cacheManager;
  }

  @Bean
  @Primary
  @Profile("test")
  public CacheManager testCacheManager() {
    return new ConcurrentMapCacheManager(CACHE_NAMES.toArray(String[]::new));
  }
}
