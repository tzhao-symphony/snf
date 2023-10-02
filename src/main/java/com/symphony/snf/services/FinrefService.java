package com.symphony.snf.services;

import com.symphony.snf.config.ExternalCallConfig;
import com.symphony.snf.model.Finref;
import com.symphony.snf.model.FinrefRequestBody;
import com.symphony.snf.model.FinrefResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class FinrefService {
  private AuthenticationService authenticationService;

  private final WebClient webClient;

  FinrefService(AuthenticationService authenticationService, ExternalCallConfig externalCallConfig) {
    webClient = WebClient.create(externalCallConfig.getHost());
    this.authenticationService = authenticationService;
  }

  Map<String, Finref> finrefs = new HashMap<>();

  public Finref getFinref(String code) {
    Finref finref = finrefs.get(code);
    if (finref != null) {
      return finref;
    }
    finref = getBestFinrefMatch(code).blockOptional().orElseGet(null);
    if (finref != null) {
      finrefs.put(code, finref);
    }
    return finref;
  }

  private Mono<Finref> getBestFinrefMatch(String code) {
    System.out.println(code);
    return fetchFinrefs(code).map(resp -> {
      Finref firstValue = null;
      Finref firstUsdValue = null;
      for (var instrument : resp.getInstruments()) {
        if (firstValue == null) {
          firstValue = instrument;
        }
        if (firstUsdValue == null && "USD".equals(instrument.getCurrency())) {
          firstUsdValue = instrument;
        }
        if ("USD".equals(instrument.getCurrency()) && "equity".equals(instrument.getKind())) {
          return instrument;
        }

      }
      if (firstUsdValue != null) {
        return firstUsdValue;
      }
      if (firstValue != null) {
        return firstValue;
      }
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
        .header("Content-Type", "application/json").retrieve().bodyToMono(FinrefResponse.class).log();
  }
}
