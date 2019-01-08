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

import java.util.concurrent.Callable;

abstract class MsalCallable<T> implements Callable<T> {
    //AuthenticationContext context;
    ClientDataHttpHeaders headers;
    AuthenticationCallback<T> callback;
    ClientApplicationBase clientApplication;

    /*
    MsalCallable(AuthenticationContext context, AuthenticationCallback<T> callback) {
        this.context = context;
        this.callback = callback;
    }
*/
    //==================================================================================================================
    MsalCallable(ClientApplicationBase clientApplication/*, AuthenticationCallback<T> callback*/) {
        this.clientApplication = clientApplication;
        this.callback = callback;
    }
    //==================================================================================================================

    abstract T execute() throws Exception;

    void logResult(T result, ClientDataHttpHeaders headers) throws Exception {
    }

    @Override
    public T call() throws Exception {
        T result = null;
        try {
            result = execute();

            logResult(result, headers);
            if (callback != null) {
                callback.onSuccess(result);
            }
        } catch (final Exception ex) {
            clientApplication.log.error(LogHelper.createMessage("Execution of " + this.getClass() + " failed.",
                    this.headers.getHeaderCorrelationIdValue()), ex);
            if (callback != null) {
                callback.onFailure(ex);
            } else {
                throw ex;
            }
        }
        return result;
    }
}
