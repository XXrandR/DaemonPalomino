package com.gpal.DaemonPalomino.network;

import reactor.netty.http.client.HttpClient;
import reactor.core.publisher.Mono;
import java.time.Duration;

public class HttpClientSender {

    private final HttpClient client;

    public HttpClientSender(HttpClient httpClient) {
        this.client = httpClient;
    }

    public <T, A> Mono<T> sendInfo(String baseUrl, Mono<T> data, Class<A> responseType) {
        return data.flatMap(value -> client.post()
                .uri(baseUrl)
                .send((req, out) -> {
                    req.responseTimeout(Duration.ofSeconds(2));
                    return out.sendString(Mono.just(value.toString()));
                })
                .responseContent()
                .aggregate()
                .asString()
                .map(response -> {
                    return (T) response;
                }));
    }

}
