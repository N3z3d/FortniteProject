package com.fortnite.pronos.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import com.fortnite.pronos.exception.BusinessException;
import com.fortnite.pronos.exception.InvalidSwapException;
import com.fortnite.pronos.exception.TeamNotFoundException;
import com.fortnite.pronos.service.admin.ErrorJournalService;

class DomainExceptionHandlerErrorJournalTest {

  private DomainExceptionHandler handler;
  private ErrorJournalService errorJournalService;
  private MockHttpServletRequest request;

  @BeforeEach
  void setUp() {
    errorJournalService = new ErrorJournalService();
    handler = new DomainExceptionHandler(errorJournalService);
    request = new MockHttpServletRequest();
    request.setRequestURI("/api/test");
  }

  @Test
  void shouldRecordBusinessExceptionToJournal() {
    handler.handleBusinessException(new BusinessException("rule violated"), request);

    assertThat(errorJournalService.getCurrentSize()).isEqualTo(1);
    var entry = errorJournalService.getRecentErrors(1, null, null).get(0);
    assertThat(entry.getExceptionType()).isEqualTo("BusinessException");
    assertThat(entry.getStatusCode()).isEqualTo(400);
  }

  @Test
  void shouldRecordTeamNotFoundExceptionToJournal() {
    handler.handleTeamNotFoundException(new TeamNotFoundException("team 42"), request);

    assertThat(errorJournalService.getCurrentSize()).isEqualTo(1);
    var entry = errorJournalService.getRecentErrors(1, null, null).get(0);
    assertThat(entry.getExceptionType()).isEqualTo("TeamNotFoundException");
    assertThat(entry.getStatusCode()).isEqualTo(404);
  }

  @Test
  void shouldRecordInvalidSwapExceptionToJournal() {
    handler.handleInvalidSwapException(new InvalidSwapException("bad swap"), request);

    assertThat(errorJournalService.getCurrentSize()).isEqualTo(1);
    var entry = errorJournalService.getRecentErrors(1, null, null).get(0);
    assertThat(entry.getExceptionType()).isEqualTo("InvalidSwapException");
    assertThat(entry.getStatusCode()).isEqualTo(400);
  }

  @Test
  void shouldRecordMultipleExceptionsToJournal() {
    handler.handleBusinessException(new BusinessException("err1"), request);
    handler.handleBusinessException(new BusinessException("err2"), request);
    handler.handleTeamNotFoundException(new TeamNotFoundException("t1"), request);

    assertThat(errorJournalService.getCurrentSize()).isEqualTo(3);
  }

  @Test
  void shouldCaptureRequestPathInJournalEntry() {
    request.setRequestURI("/api/trades/swap");
    handler.handleInvalidSwapException(new InvalidSwapException("swap failed"), request);

    var entry = errorJournalService.getRecentErrors(1, null, null).get(0);
    assertThat(entry.getPath()).isEqualTo("/api/trades/swap");
  }
}
