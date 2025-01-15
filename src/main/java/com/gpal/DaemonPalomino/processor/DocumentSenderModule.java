package com.gpal.DaemonPalomino.processor;

import java.util.Properties;

import javax.sql.DataSource;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.network.HttpClientSender;
import dagger.Module;
import dagger.Provides;
import jakarta.inject.Named;

@Module
public class DocumentSenderModule {

    private VelocityEngine velocityEngine;

    @Provides
    public VelocityEngine provideVelocityEngine() {
        if (velocityEngine == null) {
            velocityEngine = new VelocityEngine();

            velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "class,file");
            velocityEngine.setProperty("class.resource.loader.class",
                    "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            velocityEngine.setProperty("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.NullLogSystem");
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
