package com.microsoft.aad.msal4j;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * A utility class that provides retry functionality for a callable operation.
 *
 * @param <T> The type of the result returned by the callable operation.
 */
public class RetryableCall<T> {
    private final Set<Class<? extends Exception>> acceptableExceptions; // Set of exception types that are acceptable for retry.
    private final int retryCount; // Maximum number of retry attempts.
    private int initialDelayMs; // Initial delay between retries in milliseconds.

    /**
     * Constructs a RetryableCall with a default retry count of 3 and an initial delay of 500ms.
     *
     * @param acceptableExceptions A set of exception types that are acceptable for retry.
     */
    public RetryableCall(Set<Class<? extends Exception>> acceptableExceptions) {
        this(acceptableExceptions, 3);
    }

    /**
     * Constructs a RetryableCall with a specified retry count and a default initial delay of 500ms.
     *
     * @param acceptableExceptions A set of exception types that are acceptable for retry.
     * @param retryCount The maximum number of retry attempts.
     */
    public RetryableCall(Set<Class<? extends Exception>> acceptableExceptions, int retryCount) {
        this(acceptableExceptions, retryCount, 500);
    }

    /**
     * Constructs a RetryableCall with specified retry count and initial delay.
     *
     * @param acceptableExceptions A set of exception types that are acceptable for retry.
     * @param retryCount The maximum number of retry attempts.
     * @param initialDelayMs The initial delay between retries in milliseconds.
     */
    public RetryableCall(Set<Class<? extends Exception>> acceptableExceptions, int retryCount, int initialDelayMs) {
        this.acceptableExceptions = acceptableExceptions;
        this.retryCount = retryCount;
        this.initialDelayMs = initialDelayMs;
    }

    /**
     * Executes the given callable operation with retry logic.
     *
     * @param t The callable operation to execute.
     * @return The result of the callable operation.
     * @throws Exception If the operation fails after the maximum number of retries or if an exception
     *                   not in the acceptableExceptions set is thrown.
     */
    public T callWithRetry(Callable<T> t) throws Exception {
        Exception lastException;
        int attempts = 0;
        do {
            try {
                return t.call();
            } catch (Exception e) {
                lastException = e;
                if (!acceptableExceptions.contains(e.getClass())) {
                    throw e;
                }
            }
            Thread.sleep(initialDelayMs);
        } while (++attempts != retryCount);
        throw lastException;
    }

    /**
     * Gets the maximum number of retry attempts.
     *
     * @return The retry count.
     */
    public int getRetryCount() {
        return retryCount;
    }

    /**
     * Returns a string representation of the RetryableCall object.
     *
     * @return A string containing the acceptable exceptions and retry count.
     */
    @Override
    public String toString() {
        return "RetryableCall{" +
                "acceptableExceptions=" + acceptableExceptions +
                ", retryCount=" + retryCount +
                '}';
    }
}