# Managed Identity v2 kill switch

Set the process environment variable `MSAL_MI_DISABLE_IMDS_V2` to `true`
(case-insensitive) or `1` to disable Managed Identity v2 for the process.
Unset, empty, `false`, `0`, and unrecognized values leave v2 enabled.

A process restart or recycle is required after changing the environment variable
outside the process.

When enabled:

- ordinary managed identity bearer requests continue through the existing IMDS
  path;
- `withMtlsProofOfPossession()` fails closed;
- `withRequestOverMtls()` fails closed;
- a request with a minimum binding-strength floor reports that the floor cannot
  be met;
- `getManagedIdentityCapabilities()` reports `MtlsBindingStrength.NONE` without
  probing IMDS v2 or provisioning a KeyGuard key.

The switch does not silently downgrade an explicitly requested mTLS flow.
