// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.SocketException;

import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryRequestChainTest {

    private final HttpRequest httpRequest = mock(HttpRequest.class);
    private final IRequestChain next = mock(IRequestChain.class);
    private final IRetryPolicy retryPolicy = mock(IRetryPolicy.class);

    private static HttpResponse response(int statusCode) {
        return new HttpResponse().statusCode(statusCode);
    }

    @Test
    void responseRetry_SucceedsOnSecondAttempt() throws Exception {
        HttpResponse retryableResponse = response(HttpStatus.HTTP_INTERNAL_ERROR);
        HttpResponse finalResponse = response(HttpStatus.HTTP_OK);

        when(next.executeHttpRequest(any(), any(), any()))
                .thenReturn(retryableResponse)
                .thenReturn(finalResponse);
        when(retryPolicy.isRetryable(retryableResponse)).thenReturn(true);
        when(retryPolicy.isRetryable(finalResponse)).thenReturn(false);
        when(retryPolicy.getMaxRetryCount(any())).thenReturn(2);
        when(retryPolicy.getRetryDelayMs(any())).thenReturn(1);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, false);

        IHttpResponse result = chain.executeHttpRequest(httpRequest, null, null);

        assertSame(finalResponse, result);
        verify(next, times(2)).executeHttpRequest(any(), any(), any());
    }

    @Test
    void responseRetry_ExhaustsRetries_ReturnsLastResponseWithoutThrowing() throws Exception {
        HttpResponse retryableResponse = response(HttpStatus.HTTP_INTERNAL_ERROR);

        when(next.executeHttpRequest(any(), any(), any())).thenReturn(retryableResponse);
        when(retryPolicy.isRetryable(retryableResponse)).thenReturn(true);
        when(retryPolicy.getMaxRetryCount(any())).thenReturn(2);
        when(retryPolicy.getRetryDelayMs(any())).thenReturn(1);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, false);

        IHttpResponse result = chain.executeHttpRequest(httpRequest, null, null);

        assertSame(retryableResponse, result);
        // 1 initial attempt + 2 retries
        verify(next, times(3)).executeHttpRequest(any(), any(), any());
    }

    @Test
    void exceptionRetry_SucceedsOnSecondAttempt() throws Exception {
        HttpResponse finalResponse = response(HttpStatus.HTTP_OK);

        when(next.executeHttpRequest(any(), any(), any()))
                .thenThrow(new SocketException("connection reset"))
                .thenReturn(finalResponse);
        when(retryPolicy.isRetryable(finalResponse)).thenReturn(false);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, false);

        IHttpResponse result = chain.executeHttpRequest(httpRequest, null, null);

        assertSame(finalResponse, result);
        verify(next, times(2)).executeHttpRequest(any(), any(), any());
    }

    @Test
    void exceptionRetry_ExhaustsRetries_RethrowsSameException() throws Exception {
        SocketException socketException = new SocketException("connection reset");

        when(next.executeHttpRequest(any(), any(), any())).thenThrow(socketException);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, false);

        Exception thrown = assertThrows(SocketException.class,
                () -> chain.executeHttpRequest(httpRequest, null, null));

        assertSame(socketException, thrown);
        // DefaultRetryableExceptionPolicy allows exactly 1 retry: 1 initial attempt + 1 retry
        verify(next, times(2)).executeHttpRequest(any(), any(), any());
    }

    @Test
    void nonRetryableException_RethrownImmediately() throws Exception {
        SSLException sslException = new SSLException("handshake failed");

        when(next.executeHttpRequest(any(), any(), any())).thenThrow(sslException);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, false);

        Exception thrown = assertThrows(SSLException.class,
                () -> chain.executeHttpRequest(httpRequest, null, null));

        assertSame(sslException, thrown);
        verify(next, times(1)).executeHttpRequest(any(), any(), any());
    }

    @Test
    void retryDisabled_RetryableResponseIsNotRetried() throws Exception {
        HttpResponse retryableResponse = response(HttpStatus.HTTP_INTERNAL_ERROR);

        when(next.executeHttpRequest(any(), any(), any())).thenReturn(retryableResponse);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, true);

        IHttpResponse result = chain.executeHttpRequest(httpRequest, null, null);

        assertSame(retryableResponse, result);
        verify(next, times(1)).executeHttpRequest(any(), any(), any());
        verify(retryPolicy, never()).isRetryable(any());
    }

    @Test
    void retryDisabled_RetryableExceptionIsRethrownImmediately() throws Exception {
        SocketException socketException = new SocketException("connection reset");

        when(next.executeHttpRequest(any(), any(), any())).thenThrow(socketException);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, true);

        Exception thrown = assertThrows(SocketException.class,
                () -> chain.executeHttpRequest(httpRequest, null, null));

        assertSame(socketException, thrown);
        verify(next, times(1)).executeHttpRequest(any(), any(), any());
    }

    @Test
    void transitionExceptionThenResponse_ReturnsFinalResponse() throws Exception {
        HttpResponse finalResponse = response(HttpStatus.HTTP_OK);

        when(next.executeHttpRequest(any(), any(), any()))
                .thenThrow(new SocketException("connection reset"))
                .thenReturn(finalResponse);
        when(retryPolicy.isRetryable(finalResponse)).thenReturn(false);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, false);

        IHttpResponse result = chain.executeHttpRequest(httpRequest, null, null);

        assertSame(finalResponse, result);
        verify(next, times(2)).executeHttpRequest(any(), any(), any());
    }

    @Test
    void transitionResponseThenException_RethrowsImmediately() throws Exception {
        HttpResponse retryableResponse = response(HttpStatus.HTTP_INTERNAL_ERROR);
        SSLException sslException = new SSLException("handshake failed");

        when(next.executeHttpRequest(any(), any(), any()))
                .thenReturn(retryableResponse)
                .thenThrow(sslException);
        when(retryPolicy.isRetryable(retryableResponse)).thenReturn(true);
        when(retryPolicy.getMaxRetryCount(any())).thenReturn(2);
        when(retryPolicy.getRetryDelayMs(any())).thenReturn(1);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, false);

        Exception thrown = assertThrows(SSLException.class,
                () -> chain.executeHttpRequest(httpRequest, null, null));

        assertSame(sslException, thrown);
        verify(next, times(2)).executeHttpRequest(any(), any(), any());
    }

    @Test
    void exceptionRetry_NeverCallsResponseBasedRetryPolicyWithNullResponse() throws Exception {
        HttpResponse finalResponse = response(HttpStatus.HTTP_OK);

        when(next.executeHttpRequest(any(), any(), any()))
                .thenThrow(new SocketException("connection reset"))
                .thenReturn(finalResponse);
        when(retryPolicy.isRetryable(finalResponse)).thenReturn(false);

        RetryRequestChain chain = new RetryRequestChain(next, retryPolicy, false);

        chain.executeHttpRequest(httpRequest, null, null);

        // retryPolicy is only ever allowed to see a response, never a null caused by the caught exception
        verify(retryPolicy, never()).isRetryable(null);
        verify(retryPolicy, never()).getMaxRetryCount(null);
        verify(retryPolicy, never()).getRetryDelayMs(null);
    }
}
