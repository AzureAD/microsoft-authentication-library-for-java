// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

/**
 * MSAL generic exception class
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private AuthenticationErrorCode errorCode;

    /**
     * Constructor
     *
     * @param t Throwable object
     */
    public AuthenticationException(final Throwable t) {
        super(t);

        this.errorCode = AuthenticationErrorCode.UNKNOWN;
    }

    /**
     * Constructor
     *
     * @param message string error message
     */
    public AuthenticationException(final String message) {
        this(AuthenticationErrorCode.UNKNOWN, message);
    }

    public AuthenticationException(AuthenticationErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor
     *
     * @param message string error message
     * @param t Throwable object
     */
    public AuthenticationException(final String message, final Throwable t) {
        super(message, t);

        this.errorCode = AuthenticationErrorCode.UNKNOWN;
    }

    public AuthenticationErrorCode getErrorCode() {
        return errorCode;
    }
}
