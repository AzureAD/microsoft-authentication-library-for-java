package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.URI;

@Accessors(fluent = true)
@Getter
@Setter
public class SystemBrowserOptions {

    private String htmlMessageSuccess;

    private String htmlMessageError;

    private URI browserRedirectSuccess;

    private URI browserRedirectError;

    private OpenBrowserAction openBrowserAction;
}
