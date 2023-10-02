package com.symphony.snf.model;

import lombok.Getter;

@Getter
public class FinrefRequestBody {
  String queryType = "local_code";
  int from = 0;
  int size = 30;
  String searchQuery;

  public FinrefRequestBody(String searchQuery) {
    this.searchQuery = searchQuery;
  }

  FinrefRequestBody(String searchQuery, int from) {
    this(searchQuery);
    this.from = from;

  }
}
