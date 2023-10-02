package com.symphony.snf.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Finref {
  String uniqueId;
  String kind;
  String bbgCompTicker;
  String fullBbgCompTicker;
  String rootBbgCompTicker;
  String isin;
  String figi;
  String figiTicker;
  String displayName;
  String localCode;
  String usCode;
  String operationalMic;
  String exchangeName;
  String countryName;
  String currency;
  String instrumentTypeCode;
  String instrumentTypeName;
}
