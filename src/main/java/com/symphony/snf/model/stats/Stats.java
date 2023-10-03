package com.symphony.snf.model.stats;

import com.symphony.snf.model.stats.FinrefStats;
import com.symphony.snf.model.stats.NewsStats;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Stats {
FinrefStats finrefStats;
NewsStats newsStats;
}
