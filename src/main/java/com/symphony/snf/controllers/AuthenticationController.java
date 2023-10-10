package com.symphony.snf.controllers;

import com.symphony.snf.config.ExternalCallConfig;
import com.symphony.snf.services.AuthenticationService;
import com.symphony.snf.services.FinrefService;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("auth")
@AllArgsConstructor
public class AuthenticationController {

  private AuthenticationService authenticationService;

  private FinrefService finrefService;

  private ExternalCallConfig config;

  @CrossOrigin
  @PostMapping("/setKeys")
  public void setKeys(@RequestParam(value = "skey") String skey,
      @RequestParam(value = "x-symphony-anti-csrf-token") String csrfTokenHeader,
      @RequestHeader(value = "origin", required = false) String origin) {
    var previousSkey = authenticationService.getSkey();
    var previousCsrfToken = authenticationService.getAntiCsrfToken();
    var previousHost = config.getHost();

    if (StringUtils.isNotBlank(origin)) {
      config.setHost(origin);
    }

    var csrfToken = csrfTokenHeader;
    if (StringUtils.isNotBlank(skey) && StringUtils.isNotBlank(csrfToken)) {
      authenticationService.setSkey(skey);
      authenticationService.setAntiCsrfToken(csrfToken);
      boolean isRenewalSuccessful = authenticationService.renewJwt();
      if (!isRenewalSuccessful) {
        authenticationService.setSkey(previousSkey);
        authenticationService.setAntiCsrfToken(previousCsrfToken);
        config.setHost(previousHost);
      } else {
        finrefService.updateWebclient();
      }
    }
  }

  @Deprecated(since = "might be too hacky")
  @PostMapping("/setKeysFromRequest")
  public void setKeysFromRequest(@CookieValue(value = "skey", required = false) String skey,
      @RequestHeader("x-symphony-anti-csrf-token") RequestHeader csrfTokenHeader) {
    var previousSkey = authenticationService.getSkey();
    var previousCsrfToken = authenticationService.getAntiCsrfToken();

    var csrfToken = csrfTokenHeader.value();
    if (StringUtils.isNotBlank(skey) && StringUtils.isNotBlank(csrfToken)) {
      authenticationService.setSkey(skey);
      authenticationService.setAntiCsrfToken(csrfToken);
      boolean isRenewalSuccessful = authenticationService.renewJwt();
      if (!isRenewalSuccessful) {
        authenticationService.setSkey(previousSkey);
        authenticationService.setAntiCsrfToken(previousCsrfToken);
      }
    }
  }
}
