package com.gpal.DaemonPalomino.processor;

import com.gpal.DaemonPalomino.database.HikariBase;
import com.gpal.DaemonPalomino.network.HttpClientSender;

import dagger.Module;
import dagger.Provides;

@Module
public class DocumentSenderModule{

    @Provides
    public DocumentSender provideDocumentSender(HikariBase connection, HttpClientSender httpClient){
        return new DocumentSender(connection, httpClient);
    }

}
