package com.arematics.keycloak.otp.provider;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.*;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class OtpResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String PROVIDER_ID = "arematics-tfa";
    private static final Logger logger = Logger.getLogger(OtpResourceProviderFactory.class);

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void close() {
    }

    @Override
    public OtpResourceProvider create(KeycloakSession session) {
        logger.debug("OtpResourceProviderFactory::create");
        return new OtpResourceProvider(session);
    }

    @Override
    public void init(Config.Scope config) {}

    @Override
    public void postInit(KeycloakSessionFactory factory) {}
}
