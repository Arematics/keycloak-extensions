package com.arematics.keycloak.otp.resources;

import com.arematics.keycloak.otp.AbstractAdminResource;
import com.arematics.keycloak.otp.model.OtpDetailsRepresentation;
import com.arematics.keycloak.otp.model.OtpRepresentation;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.common.ClientConnection;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.forms.account.freemarker.model.TotpBean;
import org.keycloak.models.*;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.CredentialValidation;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.services.resources.admin.RealmsAdminResource;
import org.keycloak.utils.CredentialHelper;

import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class OtpResource extends AbstractAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmsAdminResource.class);
    @Context
    protected HttpRequest request;
    @Context
    protected HttpResponse response;
    @Context
    protected ClientConnection clientConnection;
    @Context
    private KeycloakSession session;

    public OtpResource(RealmModel realm) {
        super(realm);
    }

    public static final CacheControl noCache = new CacheControl();

    static {
        noCache.setNoCache(true);
    }

    @GET
    @Path("code/{uuid}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public OtpRepresentation fetchOtpEnableConfig(final @PathParam("uuid") String uuid) {
        UserModel user = session.users().getUserById(realm, uuid);
        TotpBean totp = new TotpBean(session, realm, user,
                session.getContext().getUri().getRequestUriBuilder());
        return new OtpRepresentation(uuid,
                "data:image/png;base64, " + totp.getTotpSecretQrCode(),
                totp.getTotpSecret(),
                totp.getTotpSecretEncoded());
    }

    @POST
    @Path("enable/{uuid}")
    @NoCache
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enable2Fa(final @PathParam("uuid") String uuid,
                              final OtpDetailsRepresentation optDetails) {
        String totp = optDetails.getTotp();
        OTPPolicy policy = realm.getOTPPolicy();
        UserModel user = session.users().getUserById(realm, uuid);

        OTPCredentialModel credential = OTPCredentialModel.createTOTP(optDetails.getSecret(),
                policy.getDigits(),
                policy.getPeriod(),
                policy.getAlgorithm());
        credential.setUserLabel(optDetails.getLabel());
        if (CredentialValidation.validOTP(totp, credential, policy.getLookAheadWindow())) {
            boolean success = CredentialHelper.createOTPCredential(session, realm, user, totp, credential);
            logger.debugf("%s setting up otp for user %s", success, user.getUsername());
            return Response.status(200).type(MediaType.APPLICATION_JSON).build();
        } else {
            return Response.status(400).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
