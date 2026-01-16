package com.fortnite.pronos.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fortnite.pronos.service.ingestion.PrCsvParser.ParseResult;

class PrCsvParserTddTest {
  private final PrCsvParser parser = new PrCsvParser();

  @Test
  void shouldParseValidRows() {
    String csv =
        "nickname,region,points,rank,snapshot_date\n"
            + "pixie,EU,108022,1,2025-01-10\n"
            + "Muz,NAC,125360,2,2025-01-10";

    ParseResult result = parser.parse(new StringReader(csv));

    assertThat(result.failureReason()).isNull();
    assertThat(result.errorCount()).isZero();
    assertThat(result.rows()).hasSize(2);
    assertThat(result.rows().get(0).nickname()).isEqualTo("pixie");
    assertThat(result.rows().get(0).snapshotDate()).isEqualTo(LocalDate.parse("2025-01-10"));
  }

  @Test
  void shouldFailWhenHeaderMissingRequiredColumns() {
    String csv = "nickname,points,rank,snapshot_date\n" + "pixie,108022,1,2025-01-10";

    ParseResult result = parser.parse(new StringReader(csv));

    assertThat(result.failureReason()).isEqualTo("invalid_header");
    assertThat(result.rows()).isEmpty();
  }

  @Test
  void shouldReturnSuccessWithErrorsWhenSomeRowsAreInvalid() {
    String csv =
        "nickname,region,points,rank,snapshot_date\n"
            + "pixie,EU,108022,1,2025-01-10\n"
            + "bad,UNKNOWN,100,2,2025-01-10";

    ParseResult result = parser.parse(new StringReader(csv));

    assertThat(result.failureReason()).isNull();
    assertThat(result.rows()).hasSize(1);
    assertThat(result.errorCount()).isEqualTo(1);
  }

  @Test
  void shouldFailWhenAllRowsHaveInvalidDates() {
    String csv = "nickname,region,points,rank,snapshot_date\npixie,EU,108022,1,2025-99-99";

    ParseResult result = parser.parse(new StringReader(csv));

    assertThat(result.failureReason()).isEqualTo("no_rows");
    assertThat(result.errorCount()).isEqualTo(1);
  }

  @Test
  void shouldIgnoreCommentLines() {
    String csv =
        "nickname,region,points,rank,snapshot_date\n"
            + "# This is a comment explaining the scenario\n"
            + "pixie,EU,108022,1,2025-01-10\n"
            + "  # Indented comment\n"
            + "muz,NAC,125360,2,2025-01-10";

    ParseResult result = parser.parse(new StringReader(csv));

    assertThat(result.failureReason()).isNull();
    assertThat(result.errorCount()).isZero();
    assertThat(result.rows()).hasSize(2);
    assertThat(result.rows().get(0).nickname()).isEqualTo("pixie");
    assertThat(result.rows().get(1).nickname()).isEqualTo("muz");
  }

  @Test
  void shouldIgnoreBlankLines() {
    String csv =
        "nickname,region,points,rank,snapshot_date\n"
            + "pixie,EU,108022,1,2025-01-10\n"
            + "\n"
            + "   \n"
            + "muz,NAC,125360,2,2025-01-10";

    ParseResult result = parser.parse(new StringReader(csv));

    assertThat(result.failureReason()).isNull();
    assertThat(result.rows()).hasSize(2);
  }
}
