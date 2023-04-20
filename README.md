# Arematics Keycloak Extensions
Keycloak Identity Provider Extensions used for Arematics Auth System with Keycloak

## Two-Factor-Endpoint
Project that provides an admin endpoint for having the Two Factor Authentication Setup inside a own application and having no need to setup Two Factor Authentication inside the Keycloak UI.

You still need to create an Proxy Endpoint in your Keycloak Client Application that then access the admin api. Using the Admin API directly in your frontend is **NOT** recommended.

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
The flow is first using the fetchOtp for fetching a OTP Setup Init with the QR Code Picture and then use the user input as the OtpDetailsRepresentation to save the new otp setup.

```java
    public OtpRepresentation fetchOtp(String uuid){
        URI uri = URI.create(url + "/realms/" + realm + "/arematics-tfa/");
        OtpResource resource = keycloak.proxy(OtpResource.class, uri);
        return resource.fetchOtpEnableConfig(uuid);
    }

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
```

And thats it, now you jsut need to implement your Rest Endpoints in your Keycloak Client Application and use them.
