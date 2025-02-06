package com.gpal.DaemonPalomino.models;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DetSummaryDocument {
    private String NumSummary;
    private String DateRefe;
    private String IssueDate;
    private String CompanyId;
    private String CompanyName;
    private String CompanyRuc;

    private Long LineID; // number 0..n in asc
    private String DocumentType; // def 03(bol)
    private String CoMone; // def 03(bol)
    private String NuDocu;
    private BigDecimal ImTota;
    private BigDecimal ImPaga;
    private BigDecimal ImIgv;
}
