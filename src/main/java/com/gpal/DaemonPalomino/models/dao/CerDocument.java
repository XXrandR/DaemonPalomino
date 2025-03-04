package com.gpal.DaemonPalomino.models.dao;

import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CerDocument extends GenericDocument {

    private String NuDocu;
    private String tiDocu;
    private String coEmpr;
    private String coOrig;
    private String Series;
    private Integer Number;
    private String IssueDate;
    private String IssueTime;
    private String CompanyID;
    private String CompanyName;
    private String DocumentTypeId;

}
