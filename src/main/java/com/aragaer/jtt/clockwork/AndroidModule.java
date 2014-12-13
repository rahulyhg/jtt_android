package com.aragaer.jtt.clockwork;
// vim: et ts=4 sts=4 sw=4

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

import com.aragaer.jtt.astronomy.DayIntervalCalculator;
import com.aragaer.jtt.astronomy.SscCalculator;

import android.content.Context;


@Module(injects={Clock.class, DateTimeChangeListener.class, Astrolabe.class})
public class AndroidModule {

    private final Context context;

    public AndroidModule(Context context) {
        this.context = context;
    }

    @Provides @Singleton Chime getChime() {
        return new com.aragaer.jtt.clockwork.android.Chime(context);
    }

    @Provides @Singleton Metronome getMetronome() {
        return new AndroidMetronome(context);
    }

    @Provides @Singleton DayIntervalCalculator getCalculator() {
        return new SscCalculator();
    }
}