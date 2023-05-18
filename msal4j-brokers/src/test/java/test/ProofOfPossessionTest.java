package test;

import com.microsoft.aad.msal4j.*;
import com.microsoft.aad.msal4jbrokers.MsalRuntimeBroker;
import infrastructure.SeleniumExtensions;
import labapi.LabUserProvider;
import labapi.User;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Test()
public class ProofOfPossessionTest {
    private final static String MICROSOFT_AUTHORITY_ORGANIZATIONS =
            "https://login.microsoftonline.com/organizations/";
    private final static String GRAPH_DEFAULT_SCOPE = "https://graph.windows.net/.default";

    private LabUserProvider labUserProvider;

    WebDriver seleniumDriver;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquirePopToken_WithBroker() throws Exception {
        User user = labUserProvider.getDefaultUser();

        MsalRuntimeBroker broker = new MsalRuntimeBroker();

        PublicClientApplication pca = createPublicClientApp(user, broker);

        IAuthenticationResult result = acquirePoPTokenUsernamePassword(pca, user, Collections.singleton(GRAPH_DEFAULT_SCOPE));

        //A valid PoP access token should be returned if a broker was set
        assertTokenResultNotNull(result);
        Assert.assertTrue(result.isPopAuthorization());
    }

    @Test(expectedExceptions = MsalClientException.class)
    public void acquirePopToken_WithoutBroker() throws Exception {
        User user = labUserProvider.getDefaultUser();

        PublicClientApplication pca = createPublicClientApp(user);

        //Setting UserNamePasswordParameters.proofOfPossession without enabling the broker should result in an exception when trying to get a token
        IAuthenticationResult result = acquirePoPTokenUsernamePassword(pca, user, Collections.singleton(GRAPH_DEFAULT_SCOPE));
    }

    @Test
    public void acquirePopToken_BrowserAndBroker() throws Exception {
        User user = labUserProvider.getDefaultUser();

        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();

        //First, get a non-PoP (bearer) token through a browser
        PublicClientApplication pcaWithoutBroker = createPublicClientApp(user);

        SystemBrowserOptions browserOptions =
                SystemBrowserOptions
                        .builder()
                        .openBrowserAction(new SeleniumOpenBrowserAction(user, pcaWithoutBroker))
                        .build();

        IAuthenticationResult browserResult = acquireTokenInteractive(pcaWithoutBroker, browserOptions);

        assertTokenResultNotNull(browserResult);

        seleniumDriver.quit();

        //Then, get a PoP token silently, using the cache that contains the non-PoP token
        MsalRuntimeBroker broker = new MsalRuntimeBroker();

        PublicClientApplication pcaWithBroker = createPublicClientApp(user, broker, pcaWithoutBroker.tokenCache().serialize());

        IAuthenticationResult acquireSilentResult = acquireTokenSilent(pcaWithBroker, browserResult.account());

        //Ensure that the silent request retrieved a new PoP token, rather than the cached non-Pop token
        Assert.assertNotNull(acquireSilentResult);
        Assert.assertNotEquals(acquireSilentResult.accessToken(), browserResult.accessToken());
    }

    private PublicClientApplication createPublicClientApp(User user) throws MalformedURLException {
        return PublicClientApplication.builder(user.getAppId())
                .authority(MICROSOFT_AUTHORITY_ORGANIZATIONS)
                .correlationId(UUID.randomUUID().toString())
                .build();
    }

    private PublicClientApplication createPublicClientApp(User user, MsalRuntimeBroker broker) throws MalformedURLException {
        return PublicClientApplication.builder(user.getAppId())
                .authority(MICROSOFT_AUTHORITY_ORGANIZATIONS)
                .correlationId(UUID.randomUUID().toString())
                .broker(broker)
                .build();
    }

    private PublicClientApplication createPublicClientApp(User user, MsalRuntimeBroker broker, String cache) throws MalformedURLException {
        return PublicClientApplication.builder(user.getAppId())
                .authority(MICROSOFT_AUTHORITY_ORGANIZATIONS)
                .correlationId(UUID.randomUUID().toString())
                .setTokenCacheAccessAspect(new TokenPersistence(cache))
                .broker(broker)
                .build();
    }

    private IAuthenticationResult acquirePoPTokenUsernamePassword(PublicClientApplication pca, User user, Set<String> scopes)
            throws URISyntaxException, ExecutionException, InterruptedException {
        UserNamePasswordParameters parameters = UserNamePasswordParameters.builder(
                        scopes, user.getUpn(),
                        user.getPassword().toCharArray())
                .proofOfPossession(HttpMethod.GET, new URI("http://localhost"), null)
                .build();

        return pca.acquireToken(parameters).get();
    }

    private IAuthenticationResult acquireTokenInteractive(PublicClientApplication pca, SystemBrowserOptions browserOptions)
            throws URISyntaxException {
        InteractiveRequestParameters interactiveParams = InteractiveRequestParameters
                .builder(new URI("http://localhost:8080"))
                .scopes(Collections.singleton(GRAPH_DEFAULT_SCOPE))
                .systemBrowserOptions(browserOptions)
                .build();

        return pca.acquireToken(interactiveParams).join();
    }

    private IAuthenticationResult acquireTokenSilent(PublicClientApplication pca, IAccount account) throws URISyntaxException, MalformedURLException, ExecutionException, InterruptedException {
        SilentParameters silentParams = SilentParameters.builder(Collections.singleton(GRAPH_DEFAULT_SCOPE), account)
                .proofOfPossession(HttpMethod.GET, new URI("http://localhost"), null)
                .build();

        return pca.acquireTokenSilently(silentParams).get();
    }

    private void assertTokenResultNotNull(IAuthenticationResult result) {
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    class SeleniumOpenBrowserAction implements OpenBrowserAction {

        private User user;
        private PublicClientApplication pca;

        SeleniumOpenBrowserAction(User user, PublicClientApplication pca) {
            this.user = user;
            this.pca = pca;
        }

        public void openBrowser(URL url) {
            seleniumDriver.navigate().to(url);
            runSeleniumAutomatedLogin(user, pca);
        }
    }

    void runSeleniumAutomatedLogin(User user, AbstractClientApplicationBase app) {
        SeleniumExtensions.performADLogin(seleniumDriver, user);
    }

    static class TokenPersistence implements ITokenCacheAccessAspect {
        String data;

        TokenPersistence(String data) {
            this.data = data;
        }

        @Override
        public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
            iTokenCacheAccessContext.tokenCache().deserialize(data);
        }

        @Override
        public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
            data = iTokenCacheAccessContext.tokenCache().serialize();
        }
    }
}
