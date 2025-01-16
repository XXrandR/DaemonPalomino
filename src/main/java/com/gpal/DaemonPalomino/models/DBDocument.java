package com.gpal.DaemonPalomino.models;

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
public class DBDocument extends FirmSignature {
    
	//protected String DigestValue;
	//protected String SignatureValue;
	//protected String Certificate;
	//
    private String NuDocu;
    private String DateIssue;
    private String TimeIssue;
    private String DueDate;
    private String DocumentTypeId;
    private String CurrencyTypeId;
    private String CompanyID;
    private String CompanyName;
    private String DistrictId;
	private String CodigoAnexoSunat;
    private String CityName;
    private String DepartmentDescription;
    private String DistrictDescription;
    private String EstablishmentAddress;
    private String CountryId;
    private String EstablishmentEmail;
    private String CustomerId;
    private String CustomerName;
    private String FormaPago;
	private String TotalTaxes;
	private String TaxException; // 20
    private String TotalExonerated;
    private String LineExtensionAmount;
    private String TaxInclusiveAmount;
    private String PayableAmount;
    private String InvoiceQuantity;
    private String ItemTotalValue;
    private String ItemTypeCode;
    private String PercentageIgv;
    private String NameProduct;
    private String InternalId;
    private String ItemPrice;
}
