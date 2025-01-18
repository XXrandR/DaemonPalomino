package com.gpal.DaemonPalomino.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FirmSignature {
    private String DigestValue;
    private String SignatureValue;
    private String Certificate;
}
