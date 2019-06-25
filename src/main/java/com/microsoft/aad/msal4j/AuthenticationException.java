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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Base exception type thrown when an error occurs during token acquisition.
 */
@Accessors(fluent = true)
@Getter
public class AuthenticationException extends RuntimeException {

    @Getter(value = AccessLevel.PRIVATE)
    private static final long serialVersionUID = 1L;

    /**
     * Initializes a new instance of the exception class
     * @param throwable the inner exception that is the cause of the current exception
     */
    public AuthenticationException(final Throwable throwable) {
        super(throwable);
    }

    /**
     * Initializes a new instance of the exception class
     * @param message the error message that explains the reason for the exception
     */
    public AuthenticationException(final String message) {
        super(message);
    }

    /**
     * Initializes a new instance of the exception class
     * @param message the error message that explains the reason for the exception
     * @param t the inner exception that is the cause of the current exception
     */
    public AuthenticationException(final String message, final Throwable t) {
        super(message, t);
    }
}
