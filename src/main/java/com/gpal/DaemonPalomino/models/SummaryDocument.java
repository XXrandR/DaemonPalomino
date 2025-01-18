package com.gpal.DaemonPalomino.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SummaryDocument extends FirmSignature {
    private String DateRefe;
    private String IssueDate;
    private String CompanyId;
    private String CompanyName;
    @ToString.Exclude
    List<DetSummaryDocument> documents;
}
