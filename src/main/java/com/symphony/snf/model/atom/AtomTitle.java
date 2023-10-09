package com.symphony.snf.model.atom;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JacksonXmlRootElement(localName = "title")
public class AtomTitle {
  @JacksonXmlProperty(isAttribute = true)
  String type;

  @JacksonXmlText()
  String value;
}
