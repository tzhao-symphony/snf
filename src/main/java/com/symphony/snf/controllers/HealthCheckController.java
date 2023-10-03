package com.symphony.snf.controllers;

import com.symphony.snf.model.ServiceHealth;
import com.symphony.snf.model.ServiceStatus;
import com.symphony.snf.model.stats.NewsStats;
import com.symphony.snf.model.stats.Stats;
import com.symphony.snf.services.AuthenticationService;
import com.symphony.snf.services.FinrefService;
import com.symphony.snf.stores.NewsStore;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "health", produces = "application/json")
@AllArgsConstructor
public class HealthCheckController {

  AuthenticationService authenticationService;

  NewsStore newsStore;

  FinrefService finrefService;

  @CrossOrigin
  @GetMapping(value = "/status")
  ServiceHealth getStatus() {
    ServiceHealth.ServiceHealthBuilder builder = ServiceHealth.builder();
    if (StringUtils.isBlank(authenticationService.getJwt())) {
      return builder.status(ServiceStatus.DOWN).message("Missing JWT").build();
    }
    if (StringUtils.isBlank(authenticationService.getSkey()) || StringUtils.isBlank(authenticationService.getAntiCsrfToken())) {
      return builder.status(ServiceStatus.DOWN).message("Missing credentials").build();
    }

    if (!newsStore.isInitialized()) {
      return builder.status(ServiceStatus.DOWN).message("News feed initializing").build();
    }

    return builder.status(ServiceStatus.UP).build();
  }

  @GetMapping("/stats")
  Stats getStats() {
    return Stats.builder().newsStats(newsStore.getStats()).finrefStats(finrefService.getStats()).build();
  }

}
