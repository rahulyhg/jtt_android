package com.aragaer.jtt;
// vim: et ts=4 sts=4 sw=4

import com.aragaer.jtt.clockwork.AndroidClockFactory;
import com.aragaer.jtt.clockwork.Chime;
import com.aragaer.jtt.clockwork.Clock;
import com.aragaer.jtt.clockwork.TimeDateChangeListener;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class JttService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "JTT_SERVICE";
    private final Clock clock;
    private TimeDateChangeListener timeDateChangeListener;

    public JttService() {
        clock = new Clock(new AndroidClockFactory(this));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service starting");
        clock.adjust();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);

        timeDateChangeListener = new TimeDateChangeListener(clock);
        timeDateChangeListener.register(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (key.equals(Settings.PREF_LOCATION))
            clock.adjust();
        else if (key.equals(Settings.PREF_WIDGET)
                || key.equals(Settings.PREF_LOCALE)
                || key.equals(Settings.PREF_HNAME))
            WidgetProvider.draw_all(this);
    }
}