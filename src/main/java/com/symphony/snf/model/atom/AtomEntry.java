package com.symphony.snf.model.atom;

import com.symphony.snf.model.Finref;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@JacksonXmlRootElement(localName = "entry")
@Data
@Builder
public class AtomEntry {
  private AtomTitle title;

  private AtomAuthor author;

  private AtomContent content;

  private String id;

  private String published;

  @JacksonXmlElementWrapper(localName = "stocks")
  @JacksonXmlProperty(localName = "stock")
  private List<Finref> stocks;
}
