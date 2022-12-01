package com.arematics.keycloak.otp.model;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OtpDetailsRepresentation {
    private String totp;
    private String secret;
    private String label;
}
