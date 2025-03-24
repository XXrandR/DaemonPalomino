
package com.gpal.DaemonPalomino.models.dao;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataPreVFact {
    public String NU_DOCU;
    public String TI_DOCU;
    public String CO_EMPR;
}
