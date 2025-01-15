package com.gpal.DaemonPalomino.processor;

import javax.sql.DataSource;
import org.apache.velocity.app.VelocityEngine;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.network.HttpClientSender;
import dagger.Module;
import dagger.Provides;

@Module
public class DocumentSenderModule {

    private VelocityEngine velocityEngine;

    @Provides
    public VelocityEngine provideVelocityEngine() {
        if (velocityEngine == null) {
            velocityEngine = new VelocityEngine();
            velocityEngine.setProperty(VelocityEngine.FILE_RESOURCE_LOADER_PATH, "classpath:/templates/");
            velocityEngine.init();
        }
        return velocityEngine;
    }

    @Provides
    public GenerateDocument provideGenerateDocument(VelocityEngine velocityEngine) {
        return new GenerateDocument(velocityEngine);
    }

    @Provides
    public DocumentSender provideDocumentSender(DataSource dataSource, GenerateDocument generateDocument,
            HttpClientSender httpClient) {
        return new DocumentSender(dataSource, generateDocument, httpClient);
    }

}
