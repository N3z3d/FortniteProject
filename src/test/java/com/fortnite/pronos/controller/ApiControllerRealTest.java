package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.config.TestSecurityConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ApiControllerRealTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Test
  public void testGetTradeFormDataWithUserTeddy() throws Exception {
    String url = "http://localhost:" + port + "/api/trade-form-data?user=Teddy";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("myTeam");
    assertThat(response.getBody()).contains("Team Teddy");
    assertThat(response.getBody()).contains("myPlayers");
  }

  @Test
  public void testGetTradeFormDataWithUserMarcel() throws Exception {
    String url = "http://localhost:" + port + "/api/trade-form-data?user=Marcel";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("myTeam");
    assertThat(response.getBody()).contains("Team Marcel");
    assertThat(response.getBody()).contains("myPlayers");
  }

  @Test
  public void testGetTradeFormDataWithoutUser() throws Exception {
    String url = "http://localhost:" + port + "/api/trade-form-data";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("myTeam");
    assertThat(response.getBody()).contains("Team Thibaut");
  }

  @Test
  public void testGetTradeFormDataWithInvalidUser() throws Exception {
    String url = "http://localhost:" + port + "/api/trade-form-data?user=UserInexistant";
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("error");
  }
}
