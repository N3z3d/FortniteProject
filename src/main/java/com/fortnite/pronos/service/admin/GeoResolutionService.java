package com.fortnite.pronos.service.admin;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

@Service
public class GeoResolutionService {

  static final String LOCAL = "Local";
  static final String UNKNOWN = "Unknown";
  private static final String UNKNOWN_COUNTRY_CODE = "XX";
  private static final int COUNTRY_CODE_LENGTH = 2;
  private static final String LOCALHOST_IPV6_SHORT = "::1";
  private static final String LOCALHOST_IPV6_FULL = "0:0:0:0:0:0:0:1";
  private static final String PRIVATE_172_PREFIX = "172.";
  private static final int SECOND_OCTET_START_INDEX = 4;
  private static final int MIN_PRIVATE_172_SECOND_OCTET = 16;
  private static final int MAX_PRIVATE_172_SECOND_OCTET = 31;

  public String resolveCountry(HttpServletRequest request) {
    String countryCode = resolveCountryCodeFromHeaders(request);
    if (countryCode != null) {
      return countryCode;
    }
    return isPrivateAddress(request.getRemoteAddr()) ? LOCAL : UNKNOWN;
  }

  private String resolveCountryCodeFromHeaders(HttpServletRequest request) {
    String cfCountry = request.getHeader("CF-IPCountry");
    if (isValidCountryCode(cfCountry)) {
      return cfCountry.toUpperCase(Locale.ROOT);
    }
    String xCountry = request.getHeader("X-Country-Code");
    if (isValidCountryCode(xCountry)) {
      return xCountry.toUpperCase(Locale.ROOT);
    }
    return null;
  }

  private boolean isValidCountryCode(String code) {
    return code != null
        && !code.isBlank()
        && code.length() == COUNTRY_CODE_LENGTH
        && !code.equalsIgnoreCase(UNKNOWN_COUNTRY_CODE);
  }

  private boolean isPrivateAddress(String addr) {
    if (addr == null) {
      return false;
    }
    boolean isLoopbackAddress =
        addr.equals(LOCALHOST_IPV6_SHORT) || addr.equals(LOCALHOST_IPV6_FULL);
    boolean isPrivateIpv4Prefix =
        addr.startsWith("127.") || addr.startsWith("10.") || addr.startsWith("192.168.");
    return isLoopbackAddress || isPrivateIpv4Prefix || isPrivate172(addr);
  }

  private boolean isPrivate172(String addr) {
    if (!addr.startsWith(PRIVATE_172_PREFIX)) {
      return false;
    }
    int dotIndex = addr.indexOf('.', SECOND_OCTET_START_INDEX);
    Integer secondOctet = parseSecondOctet(addr, dotIndex);
    return secondOctet != null
        && secondOctet >= MIN_PRIVATE_172_SECOND_OCTET
        && secondOctet <= MAX_PRIVATE_172_SECOND_OCTET;
  }

  private Integer parseSecondOctet(String addr, int dotIndex) {
    if (dotIndex < 0) {
      return null;
    }
    try {
      return Integer.parseInt(addr.substring(SECOND_OCTET_START_INDEX, dotIndex));
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
