package com.symphony.snf.model.atom;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JacksonXmlRootElement(localName = "content")
public class AtomContent {
  @JacksonXmlProperty(isAttribute = true)
  String type;

  @JacksonXmlText
  String value;

}
