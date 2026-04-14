// mTLS PoP E2E Test Runner
//
// Dispatches to path1 (Confidential Client) or path2 (Managed Identity) based on the
// first argument.
//
// Usage:
//   java -jar target/msal4j-mtls-extensions-1.0.0-e2e.jar path1 [options]
//   java -jar target/msal4j-mtls-extensions-1.0.0-e2e.jar path2 [--attest]
//
// Run with no arguments or --help for usage.

package com.microsoft.aad.msal4j.mtls.e2e;

import java.util.Arrays;

/**
 * Entry point for the mTLS PoP end-to-end test suite.
 * Dispatches to {@link Path1ConfidentialClient} or {@link Path2ManagedIdentity}.
 */
public class E2ETestRunner {

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            printUsage();
            return;
        }

        String path = args[0].toLowerCase();
        String[] rest = Arrays.copyOfRange(args, 1, args.length);

        switch (path) {
            case "path1":
                Path1ConfidentialClient.run(rest);
                break;
            case "path2":
                Path2ManagedIdentity.run(rest);
                break;
            default:
                System.err.println("Unknown path: " + args[0]);
                printUsage();
                System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("msal4j mTLS PoP End-to-End Test Runner");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar msal4j-mtls-extensions-*-e2e.jar <path> [options]");
        System.out.println();
        System.out.println("Paths:");
        System.out.println("  path1   Confidential Client (SNI certificate, Azure AD app registration)");
        System.out.println("  path2   Managed Identity (IMDSv2, VBS KeyGuard, Azure VM)");
        System.out.println();
        System.out.println("Path 1 options:");
        System.out.println("  --tenant <tenantId>   Azure AD tenant ID");
        System.out.println("  --client <clientId>   Azure AD app (client) ID");
        System.out.println("  --region <region>     Azure region (default: centraluseuap)");
        System.out.println("  --resource <url>      Downstream resource (default: https://graph.microsoft.com)");
        System.out.println("  --errors-only         Run only error-case validation (no Azure credentials needed)");
        System.out.println();
        System.out.println("Path 2 options:");
        System.out.println("  --attest              Enable attestation (requires AttestationClientLib.dll on PATH)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar e2e.jar path1 --errors-only");
        System.out.println("  java -jar e2e.jar path1 --tenant <tid> --client <cid> --region westus2");
        System.out.println("  java -jar e2e.jar path2 --attest");
    }
}
