package com.arematics.keycloak.otp.model;

import lombok.Data;

@Data
public class OtpRepresentation {
    private final String uuid;
    private final String image;
    private final String totpSecret;
    private final String totpSecretEncoded;
}
