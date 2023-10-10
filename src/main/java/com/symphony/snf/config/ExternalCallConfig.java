package com.symphony.snf.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class ExternalCallConfig {
  @Value("${pod_host:https://corporate.symphony.com}")
  String host;
}
