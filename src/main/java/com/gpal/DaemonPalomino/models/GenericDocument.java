package com.gpal.DaemonPalomino.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class GenericDocument extends FirmSignature {

  private String NuDocu;
  private String CompanyID;
  private String DocumentTypeId;

}
