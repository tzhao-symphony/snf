package com.symphony.snf.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class NewsItem<T> {
  protected String author;
  protected String htmlMessage;
  protected String message;
  protected String msgId;
  protected String postedAt;
  protected List<T> stocks;
}
