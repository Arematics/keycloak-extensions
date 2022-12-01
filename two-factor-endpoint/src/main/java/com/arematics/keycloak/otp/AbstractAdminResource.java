package com.arematics.keycloak.otp;

import org.jboss.logging.Logger;
import org.keycloak.common.ClientConnection;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.AdminEventBuilder;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

public abstract class AbstractAdminResource {

    private static final Logger logger = Logger.getLogger(AbstractAdminResource.class);

    @Context
    protected ClientConnection clientConnection;

    @Context
    private HttpHeaders headers;

    @Context
    private KeycloakSession session;

    protected RealmModel realm;
    protected AdminAuth auth;
    protected AdminEventBuilder adminEvent;

    public AbstractAdminResource(RealmModel realm) {
        this.realm = realm;
    }

    public void setup() {
        setupAuth();
        setupEvents();
    }

    private void setupAuth() {
        this.auth = authenticateRealmAdminRequest(headers);
    }

    protected AdminAuth authenticateRealmAdminRequest(HttpHeaders headers) {
        String tokenString = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) throw new NotAuthorizedException("Bearer");
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        }
        session.getContext().setRealm(realm);

        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .setRealm(realm)
                .setConnection(clientConnection)
                .setHeaders(headers)
                .authenticate();

        if (authResult == null) {
            logger.debug("Token not valid");
            throw new NotAuthorizedException("Bearer");
        }

        return new AdminAuth(realm, authResult.getToken(), authResult.getUser(), authResult.getClient());
    }

    private void setupEvents() {
        adminEvent = new AdminEventBuilder(session.getContext().getRealm(), auth, session, session.getContext().getConnection())
                .realm(session.getContext().getRealm());
    }

}