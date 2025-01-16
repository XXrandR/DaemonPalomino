package com.gpal.DaemonPalomino.models;


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
    private String LineID; // number 0..n in asc
    private String DocumentType; // def 03(bol)
    private String NuDocu;
    private Double ImTota;
    private Double ImPaga;
    private Double ImIgv;
}
