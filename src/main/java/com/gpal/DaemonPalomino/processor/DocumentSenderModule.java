package com.gpal.DaemonPalomino.processor;

import com.gpal.DaemonPalomino.database.HikariBase;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class DocumentSenderModule{

    @Provides
    public DocumentSender provideDocumentSender(HikariBase connection){
        return new DocumentSender(connection);
    }

}
