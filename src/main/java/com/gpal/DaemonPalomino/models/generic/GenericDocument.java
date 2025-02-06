package com.gpal.DaemonPalomino.models.generic;

import com.gpal.DaemonPalomino.models.firm.FirmSignature;
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

    // if the type of document does not require it, just ignore it and let these in
    // null, in case of Summaries for example
    private String NU_DOCU;
    private String TI_DOCU;
    private String CO_EMPR;
    private String CO_ORIG;

}
