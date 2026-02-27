package com.fortnite.pronos.service.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fortnite.pronos.config.SupabaseProperties;
import com.fortnite.pronos.service.MockDataGeneratorService.MockDataSet;
import com.fortnite.pronos.service.MockDataGeneratorService.PlayerWithScore;
import com.fortnite.pronos.service.supabase.SupabaseTableService;
import com.fortnite.pronos.service.supabase.dto.SupabasePlayerAssignmentRowDto;

@ExtendWith(MockitoExtension.class)
class SupabaseSeedDataProviderServiceTest {

  @Mock private SupabaseTableService supabaseTableService;

  @Test
  void loadSeedData_normalizesRegionAndUsernameWithLocaleRoot() {
    SupabaseSeedDataProviderService service =
        new SupabaseSeedDataProviderService(configuredProperties(), supabaseTableService);
    when(supabaseTableService.fetchPlayerAssignments())
        .thenReturn(List.of(new SupabasePlayerAssignmentRowDto("alice", "I-Test", "asia", 12, 2)));

    Locale previousLocale = Locale.getDefault();
    Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    try {
      MockDataSet result = service.loadSeedData();

      assertThat(result.total()).isEqualTo(1);
      PlayerWithScore playerWithScore = result.getPlayersFor("alice").getFirst();
      assertThat(playerWithScore.player().getUsername()).isEqualTo("i_test");
      assertThat(playerWithScore.player().getRegion())
          .isEqualTo(com.fortnite.pronos.model.Player.Region.ASIA);
    } finally {
      Locale.setDefault(previousLocale);
    }
  }

  @Test
  void loadSeedData_fallsBackWhenRegionAndNicknameInvalid() {
    SupabaseSeedDataProviderService service =
        new SupabaseSeedDataProviderService(configuredProperties(), supabaseTableService);
    when(supabaseTableService.fetchPlayerAssignments())
        .thenReturn(List.of(new SupabasePlayerAssignmentRowDto("", " ", "earth", null, null)));

    MockDataSet result = service.loadSeedData();

    PlayerWithScore playerWithScore = result.getPlayersFor("Unknown").getFirst();
    assertThat(playerWithScore.player().getUsername()).isEqualTo("unknown_user");
    assertThat(playerWithScore.player().getRegion())
        .isEqualTo(com.fortnite.pronos.model.Player.Region.EU);
    assertThat(playerWithScore.score().getPoints()).isZero();
    assertThat(playerWithScore.classement()).isZero();
  }

  @Test
  void loadSeedData_returnsEmptyWhenNotConfigured() {
    SupabaseProperties properties = new SupabaseProperties();
    SupabaseSeedDataProviderService service =
        new SupabaseSeedDataProviderService(properties, supabaseTableService);

    MockDataSet result = service.loadSeedData();

    assertThat(result.total()).isZero();
    assertThat(result.playersByPronosticator()).isEmpty();
  }

  @Test
  void loadSeedData_returnsEmptyWhenAssignmentsMissingAndSeedGameIdInvalid() {
    SupabaseProperties properties = configuredProperties();
    properties.setSeedGameId("not-a-uuid");
    SupabaseSeedDataProviderService service =
        new SupabaseSeedDataProviderService(properties, supabaseTableService);
    when(supabaseTableService.fetchPlayerAssignments()).thenReturn(List.of());

    MockDataSet result = service.loadSeedData();

    assertThat(result.total()).isZero();
    assertThat(result.playersByPronosticator()).isEmpty();
  }

  private SupabaseProperties configuredProperties() {
    SupabaseProperties properties = new SupabaseProperties();
    properties.setUrl("https://example.supabase.co");
    properties.setAnonKey("anon-key");
    return properties;
  }
}
