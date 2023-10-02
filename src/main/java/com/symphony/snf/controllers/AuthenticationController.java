package com.symphony.snf.controllers;

import com.symphony.snf.services.AuthenticationService;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
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

  @PostMapping("/setKeys")
  public void setKeys(@RequestParam(value = "skey") String skey,
      @RequestParam(value = "x-symphony-anti-csrf-token") String csrfTokenHeader) {
    var previousSkey = authenticationService.getSkey();
    var previousCsrfToken = authenticationService.getAntiCsrfToken();

    var csrfToken = csrfTokenHeader;
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
