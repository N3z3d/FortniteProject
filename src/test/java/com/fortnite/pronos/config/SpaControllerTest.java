package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fortnite.pronos.service.admin.ErrorJournalService;
import com.fortnite.pronos.service.admin.VisitTrackingService;

/**
 * Vérifie que {@link SpaController} forward correctement toutes les routes Angular vers {@code
 * index.html}, y compris les routes à 2+ segments comme {@code /admin/dashboard}.
 *
 * <p>Les routes {@code /api/**} ne sont pas interceptées (Spring Security retourne 401/403 avant
 * que SpaController soit consulté).
 */
@WebMvcTest(controllers = SpaController.class)
@Import({SecurityConfig.class, SecurityTestBeansConfig.class})
@ActiveProfiles("security-it")
@DisplayName("SpaController — SPA routing forwards")
class SpaControllerTest {

  @Autowired private MockMvc mockMvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private UserDetailsService userDetailsService;

  // VisitTrackingFilter (auto-loaded in @WebMvcTest slice) requires VisitTrackingService
  @org.springframework.boot.test.mock.mockito.MockBean
  private VisitTrackingService visitTrackingService;

  // GameExceptionHandler (@ControllerAdvice, auto-loaded) requires ErrorJournalService
  @org.springframework.boot.test.mock.mockito.MockBean
  private ErrorJournalService errorJournalService;

  // ── Root path ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("GET / → forward to index.html (root path)")
  void rootPath_shouldForwardToIndex() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
  }

  // ── Single-segment routes ─────────────────────────────────────────────────

  @Test
  @DisplayName("GET /admin → forward to index.html (single-segment admin route)")
  void adminRoute_singleSegment_shouldForwardToIndex() throws Exception {
    mockMvc
        .perform(get("/admin"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  @DisplayName("GET /catalogue → forward to index.html")
  void catalogueRoute_shouldForwardToIndex() throws Exception {
    mockMvc
        .perform(get("/catalogue"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  @DisplayName("GET /games → forward to index.html")
  void gamesRoute_shouldForwardToIndex() throws Exception {
    mockMvc
        .perform(get("/games"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  // ── Multi-segment routes (regression for the 404 bug) ────────────────────

  @Test
  @DisplayName("GET /admin/dashboard → forward to index.html (2-segment route — was 404)")
  void adminDashboard_twoSegments_shouldForwardToIndex() throws Exception {
    mockMvc
        .perform(get("/admin/dashboard"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  @DisplayName("GET /admin/pipeline → forward to index.html")
  void adminPipeline_shouldForwardToIndex() throws Exception {
    mockMvc
        .perform(get("/admin/pipeline"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  @DisplayName("GET /admin/users → forward to index.html")
  void adminUsers_shouldForwardToIndex() throws Exception {
    mockMvc
        .perform(get("/admin/users"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  @DisplayName("GET /admin/games → forward to index.html (admin sub-route)")
  void adminGames_shouldForwardToIndex() throws Exception {
    mockMvc
        .perform(get("/admin/games"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  @Test
  @DisplayName("GET /games/abc123/draft/snake → forward to index.html (3+ segments)")
  void gamesDeepRoute_threeSegments_shouldForwardToIndex() throws Exception {
    mockMvc
        .perform(get("/games/abc123/draft/snake"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/index.html"));
  }

  // ── Static files must NOT be intercepted by SpaController ───────────────

  @Test
  @DisplayName("GET /main.js → NOT forwarded to index.html (dot in name excludes static files)")
  void staticFile_withDot_shouldNotForwardToIndex() throws Exception {
    // SpaController regex [^.]* forbids dots — static files bypass SpaController entirely.
    // MockMvc returns 404 (no static resource handler in slice context), but crucially
    // the response must NOT forward to /index.html.
    String forwardedUrl =
        mockMvc.perform(get("/main.js")).andReturn().getResponse().getForwardedUrl();
    assertThat(forwardedUrl).isNotEqualTo("/index.html");
  }

  // ── API routes must NOT be intercepted by SpaController ──────────────────

  @Test
  @DisplayName("GET /api/admin/dashboard → 401/403 from SecurityConfig, not forwarded to index")
  void apiAdminRoute_shouldNotBeForwardedToIndex() throws Exception {
    int httpStatus =
        mockMvc.perform(get("/api/admin/dashboard")).andReturn().getResponse().getStatus();
    assertThat(httpStatus).isIn(401, 403);
  }
}
