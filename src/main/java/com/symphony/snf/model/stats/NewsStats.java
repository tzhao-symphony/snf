package com.symphony.snf.model.stats;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NewsStats {
  private int pageCount;
  private int newsCount;
  private String lastUpdate;
}
