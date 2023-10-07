package com.symphony.snf.model.json;

import com.symphony.snf.model.atom.AtomAuthor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class JsonEntry {
  private String title;

  private AtomAuthor author;

  private String content;

  private String id;

  private String published;

  private List<String> stocks;
}
