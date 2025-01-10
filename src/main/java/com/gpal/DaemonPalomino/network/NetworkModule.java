package com.gpal.DaemonPalomino.network;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class NetworkModule{

	@Provides
    @Singleton
	HttpClientSender provideHttpClientSender(){
		return new HttpClientSender();
	}

}
