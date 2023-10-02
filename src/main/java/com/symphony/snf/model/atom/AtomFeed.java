package com.symphony.snf.model.atom;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@JacksonXmlRootElement(localName = "feed")
public class AtomFeed {
  String id;

  AtomAuthor author;

  String published;

  AtomLink next;

  AtomLink previous;

  AtomLink current;

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "entry")
  List<AtomEntry> entries;

  @JacksonXmlProperty(isAttribute = true)
  String xmlns = "http://www.w3.org/2005/Atom";
}
