package com.gpal.DaemonPalomino.models;

import com.gpal.DaemonPalomino.models.generic.GenericDocument;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class NcdDocument extends GenericDocument {

    private String NuDocu;
    private String Series;
    private Integer Number;
    private String IssueDate;
    private String IssueTime;
    private String DueDate;
    private String CompanyID;
    private String CompanyName;
    private String DocumentTypeId;
    private String PayableAmount;

}
