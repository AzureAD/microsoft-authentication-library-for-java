// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class AuthenticationErrorMessage {

    final static String ACCESSING_METADATA_DOCUMENT_FAILED = "Accessing WS metadata exchange failed";
    final static String AUTHORITY_INVALID_URI_FORMAT = "'authority' should be in Uri format";
    final static String AUTHORITY_NOT_IN_VALID_LIST = "'authority' is not in the list of valid addresses";
    final static String AUTHORITY_URI_INSECURE = "'authority' should use the 'https' scheme";
    final static String AUTHORITY_URI_INVALID_PATH = "'authority' Uri should have at least one segment in the path (i.e. https://<host>/<path>/...)";
    final static String AUTHORIZATION_SERVER_INVALID_RESPONSE = "The authorization server returned an invalid response";
    final static String CERTIFICATE_KEY_SIZE_TOO_SMALL_TEMPLATE = "The certificate used must have a key size of at least %s bits";
    final static String EMAIL_ADDRESS_SUFFIX_MISMATCH = "No identity provider email address suffix matches the provided address";
    final static String ENCODED_TOKEN_TOO_LONG = "Encoded token size is beyond the upper limit";
    final static String FEDERATED_SERVICE_RETURNED_ERROR_TEMPLATE = "Federated service at %s returned error: %s";
    final static String IDENTITY_PROTOCOL_LOGIN_URL_NULL = "The LoginUrl property in identityProvider cannot be null";
    final static String IDENTITY_PROTOCOL_MISMATCH = "No identity provider matches the requested protocol";
    final static String IDENTITY_PROVIDER_REQUEST_FAILED = "Token request to identity provider failed. Check InnerException for more details";
    final static String INVALID_ARGUMENT_LENGTH = "Parameter has invalid length";
    final static String INVALID_AUTHENTICATE_HEADER_FORMAT = "Invalid authenticate header format";
    final static String INVALID_AUTHORITY_TYPE_TEMPLATE = "This method overload is not supported by '%s'";
    final static String INVALID_CREDENTIAL_TYPE = "Invalid credential type";
    final static String INVALID_FORMAT_PARAMETER_TEMPLATE = "Parameter '%s' has invalid format";
    final static String INVALID_TOKEN_CACHE_KEY_FORMAT = "Invalid token cache key format";
    final static String MISSING_AUTHENTICATE_HEADER = "WWW-Authenticate header was expected in the response";
    final static String MULTIPLE_TOKENS_MATCHED = "The cache contains multiple tokens satisfying the requirements. Call AcquireToken again providing more requirements (e.g. UserId)";
    final static String NO_DATA_FROM_STS = "No data received from security token service";
    final static String NULL_PARAMETER_TEMPLATE = "Parameter '%s' cannot be null";
    final static String PARSING_METADATA_DOCUMENT_FAILED = "Parsing WS metadata exchange failed";
    final static String PARSING_WS_TRUST_RESPONSE_FAILED = "Parsing WS-Trust response failed";
    final static String REDIRECT_URI_CONTAINS_FRAGMENT = "'redirectUri' must NOT include a fragment component";
    final static String SERVICE_RETURNED_ERROR = "Service returned error. Check InnerException for more details";
    final static String STS_METADATA_REQUEST_FAILED = "Metadata request to Access Control service failed. Check InnerException for more details";
    final static String STS_TOKEN_REQUEST_FAILED = "Token request to security token service failed.  Check InnerException for more details";
    final static String UNAUTHORIZED_HTTP_STATUS_CODE_EXPECTED = "Unauthorized Http Status Code (401) was expected in the response";
    final static String UNAUTHORIZED_RESPONSE_EXPECTED = "Unauthorized http response (status code 401) was expected";
    final static String UNEXPECTED_AUTHORITY_VALID_LIST = "Unexpected list of valid addresses";
    final static String UNKNOWN = "Unknown error";
    final static String UNKNOWN_USER = "Could not identify logged in user";
    final static String UNKNOWN_USER_TYPE = "Unknown User Type";
    final static String UNSUPPORTED_AUTHORITY_VALIDATION = "Authority validation is not supported for this type of authority";
    final static String UNSUPPORTED_MULTI_REFRESH_TOKEN = "This authority does not support refresh token for multiple resources. Pass null as a resource";
    final static String AUTHENTICATION_CANCELED = "User canceled authentication";
    final static String USER_MISMATCH = "User returned by service does not match the one in the request";
    final static String USER_CREDENTIAL_FOR_MANAGED_USERS_UNSUPPORTED = "UserCredential for Managed Users Unsupported";
    final static String USER_REALM_DISCOVERY_FAILED = "User realm discovery failed";
    final static String WS_TRUST_ENDPOINT_NOT_FOUND_IN_METADATA_DOCUMENT = "WS-Trust endpoint not found in metadata document";
}
