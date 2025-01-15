package com.gpal.DaemonPalomino.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class DBDocument {
    private String NuDocu;
    private String DateIssue;
    private String TimeIssue;
    private String DueDate;
    private String DocumentTypeId;
    private String CurrencyTypeId;
    private String PurchaseOrder;
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
	private String DigestValue;
	private String SignatureValue;
	private String Certificate;
}
