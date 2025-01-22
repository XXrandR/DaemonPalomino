package com.gpal.DaemonPalomino.models;

import java.util.List;
import java.util.Objects;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@ToString
@Getter
@Setter
@NoArgsConstructor
public class SummaryDocument extends GenericDocument {
    private String NumSummary;
    private String DateRefe;
    private String IssueDate;
    private String CompanyId;
    private String CompanyName;
    List<DetSummaryDocument> documents;

    public SummaryDocument(String NumSummary, String DateRefe, String IssueDate, String CompanyId, String CompanyName) {
        this.NumSummary = NumSummary;
        this.DateRefe = DateRefe;
        this.IssueDate = IssueDate;
        this.CompanyId = CompanyId;
        this.CompanyName = CompanyName;
    }

	@Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SummaryDocument that = (SummaryDocument) o;
        return Objects.equals(NumSummary, that.NumSummary) &&
               Objects.equals(DateRefe, that.DateRefe) &&
               Objects.equals(IssueDate, that.IssueDate) &&
               Objects.equals(CompanyId, that.CompanyId) &&
               Objects.equals(CompanyName, that.CompanyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(NumSummary, DateRefe, IssueDate, CompanyId, CompanyName);
    }
}

