package com.symphony.snf.model.atom;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;

import java.util.Deque;

@Data
@Builder
@JacksonXmlRootElement(localName = "feed")
public class AtomFeed {
  String id;

  AtomAuthor author;

  String published;

  AtomLink previous;

  AtomLink current;

  AtomLink next;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "entry")
  Deque<AtomEntry> entries;

  @JacksonXmlProperty(isAttribute = true)
  String xmlns = "http://www.w3.org/2005/Atom";
}
