package com.microsoft.aad.msal4j;

import lapapi.FederationProvider;
import lapapi.LabResponse;
import lapapi.LabUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import sun.net.www.protocol.http.AuthenticationInfo;

import java.util.Collections;

@Test()
public class IntegratedAuthenticationIT {
    private final static Logger LOG = LoggerFactory.getLogger(IntegratedAuthenticationIT.class);

    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication_ADFSv2019() throws Exception{

        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSv2019,
                true,
                true);
        labUserProvider.getUserPassword(labResponse.getUser());
        acquireTokenCommon(labResponse);

    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication_ADFSv4() throws Exception{

        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true,
                false);

        labUserProvider.getUserPassword(labResponse.getUser());
        acquireTokenCommon(labResponse);
    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication_ADFSv3() throws Exception{

        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV3,
                true,
                false);

        acquireTokenCommon(labResponse);
    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication_ADFSv2() throws Exception{

        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV2,
                true,
                false);

        acquireTokenCommon(labResponse);
    }

    private void acquireTokenCommon(
            LabResponse labResponse) throws Exception{
        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.AUTHORITY_ORGANIZATIONS).
                build();
        AuthenticationResult result = pca.acquireTokenByKerberosAuth(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                labResponse.getUser().getUpn()).
                get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
    }
}
