package com.fortnite.pronos.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fortnite.pronos.config.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ApiControllerRealTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void testGetTradeFormDataWithUserTeddy() throws Exception {
    mockMvc
        .perform(get("/api/trade-form-data").param("user", "Teddy"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myTeam").exists())
        .andExpect(jsonPath("$.myTeam.name").value("Team Teddy"))
        .andExpect(jsonPath("$.myPlayers").isArray());
  }

  @Test
  public void testGetTradeFormDataWithUserMarcel() throws Exception {
    mockMvc
        .perform(get("/api/trade-form-data").param("user", "Marcel"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myTeam").exists())
        .andExpect(jsonPath("$.myTeam.name").value("Team Marcel"))
        .andExpect(jsonPath("$.myPlayers").isArray());
  }

  @Test
  public void testGetTradeFormDataWithoutUser() throws Exception {
    mockMvc
        .perform(get("/api/trade-form-data"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myTeam").exists())
        .andExpect(jsonPath("$.myTeam.name").value("Team Thibaut"));
  }

  @Test
  public void testGetTradeFormDataWithInvalidUser() throws Exception {
    mockMvc
        .perform(get("/api/trade-form-data").param("user", "UserInexistant"))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").exists());
  }
}
