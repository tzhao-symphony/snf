package com.symphony.snf.model.stats;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FinrefStats {
  int finrefCounts;

  List<String> fintags;
}
