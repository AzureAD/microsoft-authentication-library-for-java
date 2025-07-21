// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface for accessing environment variables within the library.
 * <p>
 * This interface abstracts the mechanism for retrieving environment variables,
 * which allows for better testability and flexibility in different hosting environments.
 * It's primarily used within the library for accessing configuration settings and
 * credentials that may be stored in environment variables.
 */
interface IEnvironmentVariables {

    /**
     * Retrieves the value of the specified environment variable.
     * <p>
     * This method provides access to system environment variables that may contain
     * configuration settings or credentials needed for authentication.
     *
     * @param envVariable The name of the environment variable to retrieve.
     * @return The string value of the environment variable, or null if the variable is not defined.
     */
    String getEnvironmentVariable(String envVariable);
}
