package test;

import com.microsoft.aad.msal4j.HttpMethod;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.MsalClientException;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import com.microsoft.aad.msal4jbrokers.MsalRuntimeBroker;
import labapi.LabUserProvider;
import labapi.User;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.Collections;
import java.util.UUID;

@Test()
public class ProofOfPossessionTest {
    private final static String MICROSOFT_AUTHORITY_ORGANIZATIONS =
            "https://login.microsoftonline.com/organizations/";
    private final static String GRAPH_DEFAULT_SCOPE = "https://graph.windows.net/.default";

    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    //Try to acquire a POP token on Win 11 when broker is enabled. Expect: pop token
    @Test
    public void acquirePopToken_WithBroker() throws Exception {
        User user = labUserProvider.getDefaultUser();

        MsalRuntimeBroker broker = new MsalRuntimeBroker();

        PublicClientApplication pca = PublicClientApplication.builder(user.getAppId())
                .authority(MICROSOFT_AUTHORITY_ORGANIZATIONS)
                .broker(broker)
                .correlationId(UUID.randomUUID().toString())
                .build();

        UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(
                        Collections.singleton(GRAPH_DEFAULT_SCOPE), user.getUpn(),
                        user.getPassword().toCharArray())
                .proofOfPossession(HttpMethod.GET, new URI("http://localhost"), null)
                .build();

        IAuthenticationResult result = pca.acquireToken(parameters).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertTrue(result.isPopAuthorization());
    }

    //Try to acquire a pop token on Win 11 when broker is not enabled. Expect: error asking developer to use IsProofOfPosessionSupportedByClient, which would return false.
    @Test(expectedExceptions = MsalClientException.class)
    public void acquirePopToken_WithoutBroker() throws Exception {
        User user = labUserProvider.getDefaultUser();

        PublicClientApplication pca = PublicClientApplication.builder(user.getAppId())
                .authority(MICROSOFT_AUTHORITY_ORGANIZATIONS)
                .correlationId(UUID.randomUUID().toString())
                .build();

        UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(
                        Collections.singleton(GRAPH_DEFAULT_SCOPE), user.getUpn(),
                        user.getPassword().toCharArray())
                .proofOfPossession(HttpMethod.GET, new URI("http://localhost"), null)
                .build();

        //Setting UserNamePasswordParameters.proofOfPossession without enabling the broker should result in an exception
        //  when trying to get a token
        IAuthenticationResult result = pca.acquireToken(parameters).get();
    }
}
