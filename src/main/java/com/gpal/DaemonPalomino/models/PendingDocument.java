package com.gpal.DaemonPalomino.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PendingDocument{
    private String NU_DOCU;
    private String TI_DOCU;
    private String CO_EMPR;
    private String CO_ORIG;
}
