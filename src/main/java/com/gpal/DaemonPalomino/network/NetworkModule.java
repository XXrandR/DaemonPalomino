package com.gpal.DaemonPalomino.network;

import dagger.Module;
import dagger.Provides;
import reactor.netty.http.client.HttpClient;
import javax.inject.Singleton;

@Module
public class NetworkModule {

    @Provides
    @Singleton
    public HttpClient provideHttpClient() {
        return HttpClient.create()
            .metrics(true, s -> s);
    }

    @Provides
    @Singleton
    HttpClientSender provideHttpClientSender(HttpClient httpClient) {
        return new HttpClientSender(httpClient);
    }

}
