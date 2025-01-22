package com.gpal.DaemonPalomino.BaseComponent;

import com.gpal.DaemonPalomino.processor.DocumentScheduler;
import com.gpal.DaemonPalomino.processor.DocumentUnique;
import com.gpal.DaemonPalomino.processor.DocumentProcessorModule;
import com.gpal.DaemonPalomino.database.DatabaseModule;
import com.gpal.DaemonPalomino.network.NetworkModule;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = { DocumentProcessorModule.class, DatabaseModule.class, NetworkModule.class })
public interface CoreComponent{
    DocumentScheduler documentScheduler();
    DocumentUnique documentUnique();
}
