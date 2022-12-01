package com.arematics.keycloak.otp.provider;

import com.arematics.keycloak.otp.resources.OtpResource;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resource.RealmResourceProvider;

@RequiredArgsConstructor
public class OtpResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    @Override
    public void close() {
    }

    @Override
    public Object getResource() {
        RealmModel realm = session.getContext().getRealm();
        OtpResource resource = new OtpResource(realm);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        resource.setup();
        return resource;
    }
}
