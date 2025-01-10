package com.gpal.DaemonPalomino.BaseComponent;

import com.gpal.DaemonPalomino.processor.DocumentSender;
import com.gpal.DaemonPalomino.processor.DocumentSenderModule;
import com.gpal.DaemonPalomino.database.DatabaseModule;
import com.gpal.DaemonPalomino.network.NetworkModule;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = { DocumentSenderModule.class ,DatabaseModule.class, NetworkModule.class })
public interface CoreComponent{
    DocumentSender documentSender();
}
