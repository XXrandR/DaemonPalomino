package com.gpal.DaemonPalomino.models.dao;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;

@Builder
@Setter
@Data
public class DataPdfDocument {

    private String nuDocu;
    private String businessName;
    private String tiDocu;
    private String businessID;
    private String direction;
    private String centTelf;
    private String dateEmition;
    private String dateVenc;
    private String datClie;
    private String docuClie;
    private String directionClie;
    private String opExoneradas;
    private String igvAmount;
    private String totaPag;
    private String totaPagLetters;
    private String codHash;
    private String condPag;
    private String qrBase64;
    private List<DetBolPdfDocument> documents;

    @Builder
    @Data
    public static class DetBolPdfDocument {
        private String cant;
        private String noUnid;
        private String description;
        private String model;
        private String lote;
        private String serie;
        private String priceUnit;
        private String dto;
        private String total;
    }

}
