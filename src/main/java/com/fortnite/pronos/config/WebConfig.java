package com.fortnite.pronos.config;

import java.util.List;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final LoggingInterceptor loggingInterceptor;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins("http://localhost:4200", "http://localhost:4201")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);

    // Global CORS for all endpoints
    registry
        .addMapping("/**")
        .allowedOrigins("http://localhost:4200", "http://localhost:4201")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(loggingInterceptor)
        .addPathPatterns("/api/**")
        .excludePathPatterns("/api/actuator/**"); // Exclure les endpoints de monitoring
  }

  /**
   * OPTIMISATION JSON: Configuration du convertisseur JSON pour 149 joueurs - Compression optimisée
   * - Sérialisation optimisée des gros objets
   */
  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Optimisations pour de gros JSON (149 joueurs)
    mapper.configure(
        SerializationFeature.INDENT_OUTPUT, false); // Pas d'indentation = moins de bytes
    mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false); // Skip null values
    mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false); // Skip empty arrays

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
    converters.add(0, converter); // Priorité maximale
  }

  /**
   * ETAG SUPPORT: Cache intelligent côté client Évite le re-téléchargement de données non modifiées
   * (critical pour les listes de joueurs)
   */
  @Bean
  public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
    FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistrationBean =
        new FilterRegistrationBean<>();
    filterRegistrationBean.setFilter(new ShallowEtagHeaderFilter());

    // Appliquer aux endpoints lourds
    filterRegistrationBean.addUrlPatterns(
        "/players/*", "/api/leaderboard/*", "/api/games/*", "/api/teams/*");

    filterRegistrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);
    return filterRegistrationBean;
  }
}
