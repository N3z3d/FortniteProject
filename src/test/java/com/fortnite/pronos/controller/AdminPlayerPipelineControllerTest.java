package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.dto.admin.PipelineCountResponse;
import com.fortnite.pronos.dto.admin.PlayerIdentityEntryResponse;
import com.fortnite.pronos.dto.admin.RejectPlayerRequest;
import com.fortnite.pronos.dto.admin.ResolvePlayerRequest;
import com.fortnite.pronos.service.admin.PlayerIdentityPipelineService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPlayerPipelineController")
class AdminPlayerPipelineControllerTest {

  @Mock private PlayerIdentityPipelineService pipelineService;

  private AdminPlayerPipelineController controller;

  @BeforeEach
  void setUp() {
    controller = new AdminPlayerPipelineController(pipelineService);
  }

  @Nested
  @DisplayName("getUnresolved")
  class GetUnresolved {

    @Test
    @DisplayName("forwards pagination params to service")
    void forwardsPaginationParams() {
      when(pipelineService.getUnresolved(1, 20)).thenReturn(List.of(sampleResponse("UNRESOLVED")));

      var response = controller.getUnresolved(1, 20);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).hasSize(1);
      verify(pipelineService).getUnresolved(1, 20);
    }
  }

  @Nested
  @DisplayName("getResolved")
  class GetResolved {

    @Test
    @DisplayName("forwards pagination params to service")
    void forwardsPaginationParams() {
      when(pipelineService.getResolved(0, 50)).thenReturn(List.of(sampleResponse("RESOLVED")));

      var response = controller.getResolved(0, 50);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).hasSize(1);
      verify(pipelineService).getResolved(0, 50);
    }
  }

  @Nested
  @DisplayName("getCount")
  class GetCount {

    @Test
    @DisplayName("returns unresolved and resolved counts")
    void returnsCounts() {
      when(pipelineService.getCount()).thenReturn(new PipelineCountResponse(4, 12));

      var response = controller.getCount();

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().unresolvedCount()).isEqualTo(4);
      assertThat(response.getBody().resolvedCount()).isEqualTo(12);
    }
  }

  @Nested
  @DisplayName("resolve")
  class Resolve {

    @Test
    @DisplayName("uses principal name as resolvedBy")
    void usesPrincipalName() {
      UUID playerId = UUID.randomUUID();
      ResolvePlayerRequest request = new ResolvePlayerRequest(playerId, "bughaboo_fn_1204");
      Principal principal = () -> "thibaut";
      when(pipelineService.resolve(playerId, "bughaboo_fn_1204", "thibaut"))
          .thenReturn(sampleResponse("RESOLVED"));

      var response = controller.resolve(request, principal);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      verify(pipelineService).resolve(playerId, "bughaboo_fn_1204", "thibaut");
    }

    @Test
    @DisplayName("falls back to admin when principal is null")
    void fallsBackToAdmin() {
      UUID playerId = UUID.randomUUID();
      ResolvePlayerRequest request = new ResolvePlayerRequest(playerId, "alpha_fn");
      when(pipelineService.resolve(playerId, "alpha_fn", "admin"))
          .thenReturn(sampleResponse("RESOLVED"));

      controller.resolve(request, null);

      verify(pipelineService).resolve(playerId, "alpha_fn", "admin");
    }
  }

  @Nested
  @DisplayName("reject")
  class Reject {

    @Test
    @DisplayName("uses principal name as rejectedBy")
    void usesPrincipalName() {
      UUID playerId = UUID.randomUUID();
      RejectPlayerRequest request = new RejectPlayerRequest(playerId, "introuvable");
      Principal principal = () -> "thibaut";
      when(pipelineService.reject(playerId, "introuvable", "thibaut"))
          .thenReturn(sampleResponse("REJECTED"));

      var response = controller.reject(request, principal);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      verify(pipelineService).reject(playerId, "introuvable", "thibaut");
    }
  }

  private PlayerIdentityEntryResponse sampleResponse(String status) {
    return new PlayerIdentityEntryResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Bughaboo",
        "EU",
        status.equals("RESOLVED") ? "bughaboo_fn_1204" : null,
        status,
        status.equals("RESOLVED") ? 88 : 0,
        "admin",
        status.equals("RESOLVED") ? LocalDateTime.now() : null,
        status.equals("REJECTED") ? LocalDateTime.now() : null,
        status.equals("REJECTED") ? "introuvable" : null,
        LocalDateTime.now().minusHours(1));
  }
}
