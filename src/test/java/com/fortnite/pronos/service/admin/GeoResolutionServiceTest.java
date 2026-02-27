package com.fortnite.pronos.service.admin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class GeoResolutionServiceTest {

  private GeoResolutionService service;

  @BeforeEach
  void setUp() {
    service = new GeoResolutionService();
  }

  @Nested
  class CfIpCountryHeader {
    @Test
    void resolvesCfIpCountryWhenPresent() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("CF-IPCountry", "FR");
      request.setRemoteAddr("1.2.3.4");

      assertThat(service.resolveCountry(request)).isEqualTo("FR");
    }

    @Test
    void normalizesLowercaseCfHeader() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("CF-IPCountry", "fr");
      request.setRemoteAddr("1.2.3.4");

      assertThat(service.resolveCountry(request)).isEqualTo("FR");
    }

    @Test
    void ignoresCfIpCountryXxFallback() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("CF-IPCountry", "XX");
      request.addHeader("X-Country-Code", "DE");

      assertThat(service.resolveCountry(request)).isEqualTo("DE");
    }
  }

  @Nested
  class XCountryCodeHeader {
    @Test
    void resolvesXCountryCodeWhenNoCfHeader() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Country-Code", "US");
      request.setRemoteAddr("8.8.8.8");

      assertThat(service.resolveCountry(request)).isEqualTo("US");
    }

    @Test
    void normalizesMixedCaseXCountryCode() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("X-Country-Code", "Gb");

      assertThat(service.resolveCountry(request)).isEqualTo("GB");
    }
  }

  @Nested
  class PrivateAddressDetection {
    @Test
    void returnsLocalForLoopbackIpv4() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("127.0.0.1");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.LOCAL);
    }

    @Test
    void returnsLocalForLoopbackIpv6() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("::1");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.LOCAL);
    }

    @Test
    void returnsLocalFor10xRange() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("10.0.1.50");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.LOCAL);
    }

    @Test
    void returnsLocalFor192168Range() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("192.168.1.100");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.LOCAL);
    }

    @Test
    void returnsLocalFor172PrivateRange() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("172.16.0.1");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.LOCAL);
    }

    @Test
    void returnsLocalFor172MaxPrivateRange() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("172.31.255.255");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.LOCAL);
    }

    @Test
    void returnsUnknownFor172PublicRange() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("172.32.0.1");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.UNKNOWN);
    }

    @Test
    void returnsUnknownForMalformed172Address() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("172.bad.1.1");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.UNKNOWN);
    }
  }

  @Nested
  class FallbackUnknown {
    @Test
    void returnsUnknownForPublicIpWithoutHeaders() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr("8.8.8.8");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.UNKNOWN);
    }

    @Test
    void returnsUnknownWhenRemoteAddrIsNull() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setRemoteAddr(null);

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.UNKNOWN);
    }

    @Test
    void ignoresBlankCountryCode() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("CF-IPCountry", "  ");
      request.setRemoteAddr("8.8.8.8");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.UNKNOWN);
    }

    @Test
    void ignoresSingleCharCountryCode() {
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader("CF-IPCountry", "F");
      request.setRemoteAddr("8.8.8.8");

      assertThat(service.resolveCountry(request)).isEqualTo(GeoResolutionService.UNKNOWN);
    }
  }
}
