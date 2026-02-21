package com.fortnite.pronos.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fortnite.pronos.service.admin.VisitTrackingService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VisitTrackingFilter extends OncePerRequestFilter {

  private final VisitTrackingService visitTrackingService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    visitTrackingService.recordRequest(request);
    filterChain.doFilter(request, response);
  }
}
