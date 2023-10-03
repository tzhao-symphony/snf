package com.symphony.snf.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServiceHealth {
  private ServiceStatus status;

  private String message;
}
