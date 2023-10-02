package com.symphony.snf.services;


import com.symphony.snf.config.ExternalCallConfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Getter
@Setter
public class AuthenticationService {

  final static private String LOGIN_REFRESH_URL = "/login/refresh_token";

  final static private String JWT_REFRESH_URL = "/login/idm/tokens";

  final static private String SKEY = "skey";

  final static private String ANTI_CSRF_COOKIE = "anti-csrf-cookie";

  final static private String ANTI_CSRF_TOKEN = "x-symphony-csrf-token";

  @Autowired
  ExternalCallConfig externalCallConfig;

  @Value("${skey:}")
  String skey;

  @Value("${anti_csrf_token:}")
  String antiCsrfToken;

  String jwt;


  @Scheduled(fixedDelay = 1, initialDelay = 1, timeUnit = TimeUnit.HOURS)
  public void renewSkey() {
    if (StringUtils.isBlank(skey) || StringUtils.isBlank(antiCsrfToken)) {
      return;
    }

    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.COOKIE, SKEY + "=" + skey);
    headers.set(ANTI_CSRF_TOKEN, antiCsrfToken);

    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<Object> response =
        restTemplate.exchange(externalCallConfig.getHost() + LOGIN_REFRESH_URL, HttpMethod.GET, entity, Object.class);

    System.out.println("youhou, renewed");

    var cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    for (var cookie: cookies) {
      var cookieArray = cookie.split("=");
      var cookieName = cookieArray[0].trim();
      var cookieValue = cookieArray[1].split(";")[0].trim();
      if (ANTI_CSRF_COOKIE.equals(cookieName)) {
       this.setAntiCsrfToken(cookieValue);
       System.out.println("new csrf token:" + cookieValue);
      } else if (SKEY.equals((cookieName))) {
        this.setSkey(cookieValue);
        System.out.println("new skey:" + cookieValue);
      }
    }

    this.renewJwt();
  }

  @Scheduled(fixedDelay = 4 * 60 * 1000)
  public boolean renewJwt() {
    RestTemplate restTemplate = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.set(HttpHeaders.COOKIE, SKEY + "=" + skey);
    headers.set(ANTI_CSRF_TOKEN, antiCsrfToken);

    HttpEntity<String> entity = new HttpEntity<>(headers);
    ResponseEntity<Map> response =
        restTemplate.exchange(externalCallConfig.getHost() + JWT_REFRESH_URL, HttpMethod.POST, entity, Map.class);

    if (response.getStatusCode().is2xxSuccessful()) {
      this.setJwt((String) response.getBody().get("access_token"));
      System.out.println("Renewing JWT:" + response.getBody().get("access_token"));

      return true;
    } else {
      return false;
    }
  }
}
