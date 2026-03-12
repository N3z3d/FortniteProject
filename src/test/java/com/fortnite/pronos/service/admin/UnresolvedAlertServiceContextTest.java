package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.fortnite.pronos.domain.port.out.PlayerIdentityRepositoryPort;

@SpringJUnitConfig(
    classes = {UnresolvedAlertService.class, UnresolvedAlertServiceContextTest.Config.class})
@DisplayName("UnresolvedAlertService Spring context")
class UnresolvedAlertServiceContextTest {

  @Autowired private UnresolvedAlertService service;

  @Test
  @DisplayName("creates bean with default UTC clock constructor")
  void createsBeanWithAutowiredConstructor() {
    assertThat(service).isNotNull();
  }

  @Configuration
  static class Config {

    @Bean
    PlayerIdentityRepositoryPort playerIdentityRepositoryPort() {
      return mock(PlayerIdentityRepositoryPort.class);
    }
  }
}
