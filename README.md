# Arematics Keycloak Extensions

Keycloak Identity Provider Extensions used for Arematics Auth System with Keycloak

## Two-Factor-Endpoint

Project that provides an admin endpoint for having the Two Factor Authentication Setup inside a own application and
having no need to setup Two Factor Authentication inside the Keycloak UI.

### Deployment on Keycloak
To use the Two-Factor-Endpoint first of all you need to get a jar from the Releases Section. Also you can create the jar for the project by urself. For that clone the project and execute

```bash
./gradlew build
```

After that the used jar, if u created the jar by your own from `two-factor-endpoint/build/libs/`, must be placed inside the Keycloak Application under `otp/keycloak/providers`

Restart your Keycloak Application and have a look in https://your.keycloak.adress/auth/admin/master/console/#/master/providers if you can find `arematics-tfa` under the realm-restapi-extension section. If so, the endpoints are ready to use.

### Keycloak Client Application Setup
You still need to create an Proxy Endpoint in your Keycloak Client Application that then access the admin api. Using the
Admin API directly in your frontend is **NOT** recommended.

To use the Endpoints in your Keycloak Client Application some extra code is needed.
Follow these steps to use the endpoints.

Libraries used for this example:

```groovy
dependencies {
    implementation 'org.projectlombok:lombok:1.18.26'
    annotationProcessor 'org.projectlombok:lombok:1.18.26'
    implementation group: 'org.keycloak', name: 'keycloak-admin-client', version: '20.0.3'
    implementation group: 'org.jboss.resteasy', name: 'resteasy-client', version: '5.0.2.Final'
}
```

### Create the OtpResource Endpoint Adapters:

```java
package your.keycloak.client.resources;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public interface OtpResource {
    @GET
    @Path("code/{uuid}")
    @Produces(MediaType.APPLICATION_JSON)
    OtpRepresentation fetchOtpEnableConfig(final @PathParam("uuid") String uuid);

    @POST
    @Path("enable/{uuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    Response enable2Fa(final @PathParam("uuid") String uuid, final OtpDetailsRepresentation optDetails);
}
```

### Create the OtpRepresentation and OtpDetailsRepresentation Classes

```java
package your.keycloak.client.resources;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OtpRepresentation {
    private String uuid;
    private String image;
    private String totpSecret;
    private String totpSecretEncoded;
}
```

```java
package your.keycloak.client.resources;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class OtpDetailsRepresentation {
    private String totp;
    private String secret;
    private String label;

    @Override
    public String toString() {
        return "OtpDetailsRepresentation{" +
                "totp='" + totp + '\'' +
                ", secret='" + secret + '\'' +
                ", label='" + label + '\'' +
                '}';
    }
}
```

### Use it

The flow is first using the fetchOtp for fetching a OTP Setup Init with the QR Code Picture and then use the user input
as the OtpDetailsRepresentation to save the new otp setup.

```java
package your.keycloak.client.service;

import org.keycloak.admin.client.Keycloak;
import your.keycloak.client.resources.OtpDetailsRepresentation;
import your.keycloak.client.resources.OtpRepresentation;
import your.keycloak.client.resources.OtpResource;

import lombok.RequiredArgsConstructor;

import javax.ws.rs.core.Response;
import java.net.URI;

@RequiredArgsConstructor
public class YourAdminAccessService {
    //Your Keycloak Class from Keycloak Admin Client
    private final Keycloak keycloak;
    //Your Keycloak Auth Server Root Url
    private final String url;
    //Keycloak Realm you are using for this service
    private final String realm;


    /**
     * Initial OTP Setup fetching QR Code and Totp Secret from Two-Factor-Endpoint Library
     * @param uuid User ID for the OTP Setup
     * @return Data like QR Code and Totp Secret
     */
    public OtpRepresentation fetchOtp(String uuid){
        URI uri = URI.create(url + "/realms/" + realm + "/arematics-tfa/");
        OtpResource resource = keycloak.proxy(OtpResource.class, uri);
        return resource.fetchOtpEnableConfig(uuid);
    }

    /**
     * Add the OTP to the Keycloak User with the given data
     * @param uuid User ID for the OTP Setup
     * @param details Data containing wanted OTP Name, Secret from OTP App and Totp Secret
     */
    public void saveOtp(String uuid, OtpDetailsRepresentation details){
        URI uri = URI.create(url + "/realms/" + realm + "/arematics-tfa/");
        OtpResource resource = keycloak.proxy(OtpResource.class, uri);
        try(Response response = resource.enable2Fa(uuid, details)) {
            if(response.getStatus() == 400) throw new RuntimeException("Setup failed");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
```

And thats it, now you just need to implement your Rest Endpoints in your Keycloak Client Application and use them.
