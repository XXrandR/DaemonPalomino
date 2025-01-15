package com.gpal.DaemonPalomino.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class FirmSignature {
    protected String DigestValue;
    protected String SignatureValue;
    protected String Certificate;
}
