package com.symphony.snf.model.atom;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JacksonXmlRootElement(localName = "link")
public class AtomLink {
  @JacksonXmlProperty(isAttribute = true)
  String rel;

  @JacksonXmlProperty(isAttribute = true)
  String href;

  @JacksonXmlProperty(isAttribute = true)
  String type;
}
