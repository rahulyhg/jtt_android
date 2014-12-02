package com.aragaer.jtt;
// vim: et ts=4 sts=4 sw=4

import com.aragaer.jtt.clockwork.AndroidClock;
import com.aragaer.jtt.clockwork.Chime;
import com.aragaer.jtt.clockwork.BroadcastClockEvent;
import com.aragaer.jtt.clockwork.Clock;
import com.aragaer.jtt.clockwork.TimeDateChangeReceiver;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class JttService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "JTT_SERVICE";
    private JttStatus status_notify;
    private final Clock clock;
    private TimeDateChangeReceiver timeDateChangeReceiver;

    public JttService() {
        clock = AndroidClock.createFromContext(this);
        BroadcastClockEvent event = new BroadcastClockEvent(this, Chime.ACTION_JTT_TICK, 1);
        clock.addClockEvent(event);
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

        toggle_notify(pref.getBoolean("jtt_notify", true));

        timeDateChangeReceiver = new TimeDateChangeReceiver(clock);
        timeDateChangeReceiver.register(this);
    }

    private void toggle_notify(final boolean notify) {
        if (status_notify == null) {
            if (notify)
                status_notify = new JttStatus(this);
        } else {
            if (!notify) {
                status_notify.release();
                status_notify = null;
            }
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        if (key.equals(Settings.PREF_NOTIFY))
            toggle_notify(pref.getBoolean("jtt_notify", true));
        else if (key.equals(Settings.PREF_LOCATION))
            clock.adjust();
        else if (key.equals(Settings.PREF_WIDGET)
                || key.equals(Settings.PREF_LOCALE)
                || key.equals(Settings.PREF_HNAME))
            WidgetProvider.draw_all(this);
    }
}
