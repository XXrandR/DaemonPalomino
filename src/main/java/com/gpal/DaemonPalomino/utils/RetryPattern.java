package com.gpal.DaemonPalomino.utils;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryPattern {
    private final Retry retry;

    public RetryPattern(String retryName, int MaxAttempts, Duration wait,
            Class<? extends Throwable> retryExceptions) {

        Predicate<Throwable> retryOnExceptionPredicate = throwable -> {
            if (throwable instanceof Exception) {
                log.info("ERROR: DEADLOCK");
                return true;
            } else {
                log.info("NORMAL");
                return false;
            }
        };

        RetryConfig config = RetryConfig.custom()
                .retryOnException(retryOnExceptionPredicate)
                .maxAttempts(MaxAttempts)
                .waitDuration(wait)
                .retryExceptions(retryExceptions)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        this.retry = registry.retry(retryName, config);
        this.retry.getEventPublisher().onRetry(event -> log.info(
                "Retry attempt: " + event.getNumberOfRetryAttempts() + " - Last Error: "
                        + event.getLastThrowable().getMessage()));
        this.retry.getEventPublisher().onError(event -> log.info(
                "Retry error: " + event.getNumberOfRetryAttempts() + " - " + event.getLastThrowable().getMessage()));
        this.retry.getEventPublisher().onIgnoredError(event -> log.info(
                "Retry ignored error: nro. " + event.getNumberOfRetryAttempts()));
        this.retry.getEventPublisher().onSuccess(event -> log.info(
                "Retry success: " + event.getNumberOfRetryAttempts()));

    }

    public <T> T executeWithRetry(Supplier<T> supplier) {
        return Retry.decorateSupplier(retry, supplier).get();
    }

    public <T> T executeWithRetry(Supplier<T> supplier, Predicate<Throwable> retryOnException) {
        RetryConfig config = retry.getRetryConfig();
        Retry newRetry = RetryRegistry.of(config)
                .retry(retry.getName() + "-custom", config);
        return Retry.decorateSupplier(newRetry, supplier).get();
    }
}
