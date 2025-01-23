package com.gpal.DaemonPalomino.processor;

import javax.sql.DataSource;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import com.gpal.DaemonPalomino.builders.FirmDocument;
import com.gpal.DaemonPalomino.builders.GenerateDocument;
import com.gpal.DaemonPalomino.builders.SummaryDocumentProcess;
import com.gpal.DaemonPalomino.network.WsService;
import dagger.Module;
import dagger.Provides;

@Module
public class DocumentProcessorModule {

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

    // modules basic
    @Provides
    public GenerateDocument provideGenerateDocument(VelocityEngine velocityEngine) {
        return new GenerateDocument(velocityEngine);
    }

    @Provides
    public FirmDocument provideFirmDocument(VelocityEngine velocityEngine) {
        return new FirmDocument(velocityEngine);
    }

    @Provides
    public SummaryDocumentProcess provideSummaryDocumentProcess(VelocityEngine velocityEngine){
        return new SummaryDocumentProcess(velocityEngine);
    }

    // face to the App
    @Provides
    public DocumentScheduler provideDocumentScheduler(SummaryDocumentProcess sDocumentProcess,FirmDocument firmDocument, DataSource dataSource, GenerateDocument generateDocument,WsService wsService) {
        return new DocumentScheduler(sDocumentProcess,firmDocument,dataSource, generateDocument,wsService);
    }

    @Provides
    public DocumentUnique provideDocumentUnique(GenerateDocument generateDocument,DataSource dataSource, FirmDocument firmDocument) {
        return new DocumentUnique(generateDocument,dataSource,firmDocument);
    }

    @Provides
    public DocumentAnulate provideDocumentAnulate(DataSource dataSource,WsService wsService) {
        return new DocumentAnulate(dataSource, wsService);
    }

}
