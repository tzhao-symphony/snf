package com.symphony.snf.model.json;

import com.symphony.snf.model.Finref;
import com.symphony.snf.model.atom.AtomAuthor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Deque;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class JsonFeed {
  String id;

  AtomAuthor author;

  String published;

  String previous;

  String current;

  String next;

  Deque<JsonEntry> entries;

  Map<String, Finref> fintagDict;
}
