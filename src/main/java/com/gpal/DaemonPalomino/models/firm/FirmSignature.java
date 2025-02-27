package com.gpal.DaemonPalomino.models.firm;

import lombok.Getter;
import lombok.Setter;

// Used to store the Digest the Signature and Certificate
@Getter
@Setter
public class FirmSignature {
    private String DigestValue;
    private String SignatureValue;
    private String Certificate;
}
