package com.fortnite.pronos.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fortnite.pronos.dto.admin.NavigationTrackingRequestDto;
import com.fortnite.pronos.service.admin.VisitTrackingService;

@ExtendWith(MockitoExtension.class)
class VisitTrackingControllerTest {

  @Mock private VisitTrackingService visitTrackingService;
  @Mock private HttpServletRequest httpServletRequest;

  private VisitTrackingController controller;

  @BeforeEach
  void setUp() {
    controller = new VisitTrackingController(visitTrackingService);
  }

  @Test
  void shouldTrackFrontendNavigationAndReturnSuccess() {
    NavigationTrackingRequestDto request = new NavigationTrackingRequestDto("/games/abc");

    var response = controller.trackNavigation(request, httpServletRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    verify(visitTrackingService).recordFrontendNavigation(httpServletRequest, "/games/abc");
  }
}
