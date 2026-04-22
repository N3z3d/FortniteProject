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

import com.fortnite.pronos.dto.admin.AdapterInfoResponse;
import com.fortnite.pronos.dto.admin.CorrectMetadataRequest;
import com.fortnite.pronos.dto.admin.EpicIdSuggestionResponse;
import com.fortnite.pronos.dto.admin.PipelineCountResponse;
import com.fortnite.pronos.dto.admin.PipelineRegionalStatsDto;
import com.fortnite.pronos.dto.admin.PlayerIdentityEntryResponse;
import com.fortnite.pronos.dto.admin.RejectPlayerRequest;
import com.fortnite.pronos.dto.admin.ResolvePlayerRequest;
import com.fortnite.pronos.service.admin.AdminAuditLogService;
import com.fortnite.pronos.service.admin.PlayerIdentityPipelineService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPlayerPipelineController")
class AdminPlayerPipelineControllerTest {

  @Mock private PlayerIdentityPipelineService pipelineService;
  @Mock private AdminAuditLogService auditLogService;

  private AdminPlayerPipelineController controller;

  @BeforeEach
  void setUp() {
    controller = new AdminPlayerPipelineController(pipelineService, auditLogService);
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
      verify(auditLogService)
          .recordAction(
              "thibaut",
              "RESOLVE_PLAYER",
              "PLAYER_IDENTITY",
              playerId.toString(),
              "epicId=bughaboo_fn_1204");
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
      verify(auditLogService)
          .recordAction(
              "thibaut",
              "REJECT_PLAYER",
              "PLAYER_IDENTITY",
              playerId.toString(),
              "reason=introuvable");
    }
  }

  @Nested
  @DisplayName("getRegionalStatus")
  class GetRegionalStatus {

    @Test
    @DisplayName("returns 200 with regional stats list")
    void returnsRegionalStatsList() {
      LocalDateTime last = LocalDateTime.of(2025, 2, 28, 8, 0);
      List<PipelineRegionalStatsDto> stats =
          List.of(new PipelineRegionalStatsDto("EU", 10L, 90L, 2L, 102L, last));
      when(pipelineService.getRegionalStats()).thenReturn(stats);

      var response = controller.getRegionalStatus();

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).hasSize(1);
      assertThat(response.getBody().get(0).region()).isEqualTo("EU");
      assertThat(response.getBody().get(0).unresolvedCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("returns 200 with empty list when no data")
    void returnsEmptyList() {
      when(pipelineService.getRegionalStats()).thenReturn(List.of());

      var response = controller.getRegionalStatus();

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isEmpty();
    }
  }

  @Nested
  @DisplayName("correctMetadata")
  class CorrectMetadata {

    @Test
    @DisplayName("uses principal name as correctedBy")
    void usesPrincipalName() {
      UUID playerId = UUID.randomUUID();
      CorrectMetadataRequest request = new CorrectMetadataRequest(playerId, "NewName", "NAW");
      Principal principal = () -> "thibaut";
      when(pipelineService.correctMetadata(playerId, "NewName", "NAW", "thibaut"))
          .thenReturn(sampleResponse("UNRESOLVED"));

      var response = controller.correctMetadata(playerId, request, principal);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      verify(pipelineService).correctMetadata(playerId, "NewName", "NAW", "thibaut");
      verify(auditLogService)
          .recordAction(
              "thibaut",
              "CORRECT_METADATA",
              "PLAYER_IDENTITY",
              playerId.toString(),
              "username=NewName, region=NAW");
    }

    @Test
    @DisplayName("falls back to admin when principal is null")
    void fallsBackToAdmin() {
      UUID playerId = UUID.randomUUID();
      CorrectMetadataRequest request = new CorrectMetadataRequest(playerId, null, "EU");
      when(pipelineService.correctMetadata(playerId, null, "EU", "admin"))
          .thenReturn(sampleResponse("UNRESOLVED"));

      var response = controller.correctMetadata(playerId, request, null);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      verify(pipelineService).correctMetadata(playerId, null, "EU", "admin");
    }
  }

  @Nested
  @DisplayName("suggestEpicId")
  class SuggestEpicId {

    @Test
    @DisplayName("returns 200 with suggestion when API finds player")
    void returns_suggestion_when_found() {
      UUID playerId = UUID.randomUUID();
      EpicIdSuggestionResponse suggestion =
          new EpicIdSuggestionResponse("abc123epicid", "Bugha", 90, true);
      when(pipelineService.suggestEpicId(playerId)).thenReturn(suggestion);

      var response = controller.suggestEpicId(playerId);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().found()).isTrue();
      assertThat(response.getBody().suggestedEpicId()).isEqualTo("abc123epicid");
      assertThat(response.getBody().confidenceScore()).isEqualTo(90);
      verify(pipelineService).suggestEpicId(playerId);
    }

    @Test
    @DisplayName("returns 200 with notFound response when API has no match")
    void returns_not_found_response() {
      UUID playerId = UUID.randomUUID();
      when(pipelineService.suggestEpicId(playerId)).thenReturn(EpicIdSuggestionResponse.notFound());

      var response = controller.suggestEpicId(playerId);

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().found()).isFalse();
      assertThat(response.getBody().suggestedEpicId()).isNull();
      assertThat(response.getBody().confidenceScore()).isEqualTo(0);
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
        LocalDateTime.now().minusHours(1),
        null,
        null,
        null,
        null);
  }

  @Nested
  @DisplayName("getAdapterInfo")
  class GetAdapterInfo {

    @Test
    @DisplayName("returns 200 with stub adapter name")
    void returns_stub_adapter_info() {
      when(pipelineService.getAdapterInfo()).thenReturn(new AdapterInfoResponse("stub"));

      var response = controller.getAdapterInfo();

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().adapter()).isEqualTo("stub");
      verify(pipelineService).getAdapterInfo();
    }

    @Test
    @DisplayName("returns 200 with fortnite-api adapter name")
    void returns_fortnite_api_adapter_info() {
      when(pipelineService.getAdapterInfo()).thenReturn(new AdapterInfoResponse("fortnite-api"));

      var response = controller.getAdapterInfo();

      assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
      assertThat(response.getBody().adapter()).isEqualTo("fortnite-api");
    }
  }
}
