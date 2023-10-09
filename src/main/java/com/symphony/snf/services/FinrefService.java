package com.symphony.snf.services;

import com.symphony.snf.config.ExternalCallConfig;
import com.symphony.snf.model.Finref;
import com.symphony.snf.model.FinrefRequestBody;
import com.symphony.snf.model.FinrefResponse;
import com.symphony.snf.model.stats.FinrefStats;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FinrefService {
  private static long CACHE_DURATION_IN_HOURS = 10;
  private AuthenticationService authenticationService;

  private final WebClient webClient;

  FinrefService(AuthenticationService authenticationService, ExternalCallConfig externalCallConfig) {
    webClient = WebClient.create(externalCallConfig.getHost());
    this.authenticationService = authenticationService;
  }

  Map<String, Finref> finrefs = new HashMap<>();

  Map<String, Instant> lastFinrefOccurence = new HashMap<>();

  @Scheduled(fixedDelay = 4, timeUnit = TimeUnit.HOURS)
  private void clearUnusedFinrefs() {
    Instant now = Instant.now();
    for (Iterator<Entry<String, Instant>> it = lastFinrefOccurence.entrySet().iterator(); it.hasNext();) {
      Entry<String, Instant> entry = it.next();
      if (Duration.between(entry.getValue(), now).toHours() > CACHE_DURATION_IN_HOURS) {
        log.info("[Finref Clean Up][{}] Removing finref {} last used at {}.", now, entry.getKey(), entry.getValue());
        String key = entry.getKey();
        it.remove();
        finrefs.remove(key);
      }
    }
  }

  public Finref getFinref(String code) {
    Finref finref = finrefs.get(code);
    lastFinrefOccurence.put(code, Instant.now());
    if (finref != null) {
      return finref;
    }
    finref = getBestFinrefMatch(code).blockOptional().orElseGet(null);
    if (finref != null) {
      finrefs.put(code, finref);
    }
    return finref;
  }

  public FinrefStats getStats() {
    return FinrefStats.builder().finrefCounts(finrefs.size()).fintags(List.copyOf(finrefs.keySet())).build();
  }

  private Mono<Finref> getBestFinrefMatch(String code) {
    return fetchFinrefs(code).map(resp -> {
      Finref firstValue = null;
      Finref firstUsdValue = null;
      for (var instrument : resp.getInstruments()) {
        if (!code.equals(instrument.getLocalCode())) {
          continue;
        }

        if (firstValue == null) {
          firstValue = instrument;
        }
        if (firstUsdValue == null && "USD".equals(instrument.getCurrency())) {
          firstUsdValue = instrument;
        }
        if ("USD".equals(instrument.getCurrency()) && "equity".equals(instrument.getKind())) {
          log.info("Found perfect match for fintag {}: {}", code, instrument.getFullBbgCompTicker());
          return instrument;
        }

      }
      if (firstUsdValue != null) {
        log.warn("Fall back to first USD match for fintag {}: {}", code, firstUsdValue.getFullBbgCompTicker());
        return firstUsdValue;
      }
      if (firstValue != null) {
        log.warn("Fallback to first match for fintag {}: {}", code, firstValue.getFullBbgCompTicker());
        return firstValue;
      }
      log.warn("No match found for fintag {}, using dummy object", code);
      Finref dummyValue = new Finref();
      dummyValue.setLocalCode(code);
      return dummyValue;
    });
  }

  private Mono<FinrefResponse> fetchFinrefs(String code) {
    return webClient.post()
        .uri("/finref/api/v1/instruments")
        .bodyValue(new FinrefRequestBody(code))
        .header("Authorization", "Bearer " + authenticationService.getJwt())
        .header("Content-Type", "application/json").retrieve().bodyToMono(FinrefResponse.class);
  }
}
