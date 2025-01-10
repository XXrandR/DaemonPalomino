package com.gpal.DaemonPalomino.database;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class DatabaseModule{

    @Provides
    @Singleton
    public HikariBase provideHiraki(){
        return new HikariBase();
    }

}
