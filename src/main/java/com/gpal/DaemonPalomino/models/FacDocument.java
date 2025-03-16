package com.gpal.DaemonPalomino.models;

import java.math.BigDecimal;
import com.gpal.DaemonPalomino.models.generic.GenericDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class FacDocument extends GenericDocument {

    private String NuDocu;
    private String Series;
    private Integer Number;
    private String IssueDate;
    private String IssueTime;
    private String DueDate;
    private String AmountInLetters;
    private String Afecto;
    private String CurrencyCode;
    private String CompanyID;
    private String CompanyName;
    private String DocumentTypeId;
    private String EstablishmentUbigeo;
    private String EstablishmentAnnexCode;
    private String EstablishmentProvinceName;
    private String EstablishmentDepartamentName;
    private String EstablishmentDistrictName;
    private String EstablishmentAddress;
    private String CustomerId;
    private String CustomerName;
    private String PaymentConditionId;
    private BigDecimal TaxAmount;
    private BigDecimal TotalAmount;
    private BigDecimal PayableAmount;
    private String Description;
    private String FechaCreditDueDate;
    private String Detraccion;
    private String BankAccountDetraction;
    private Integer PercentDetraccion;
    private BigDecimal AmountDetraccion;
    //https://docs.factpro.la/catalogo-54-codigos-de-bienes-y-servicios-sujetos-a-detracciones
    private Integer CodMotivoDetraccion;
    private String DescriptionFact;

}
