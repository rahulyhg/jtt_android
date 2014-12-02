package com.aragaer.jtt.clockwork;
// vim: et ts=4 sts=4 sw=4

import com.aragaer.jtt.astronomy.DayInterval;
import com.aragaer.jtt.core.JttTime;


public class Clock {
    private final Astrolabe astrolabe;
    private final Chime chime;
    private final Clockwork clockwork;
    private final Metronome metronome;

    public Clock(Astrolabe astrolabe, Chime chime, Metronome metronome) {
        this.astrolabe = astrolabe;
        this.chime = chime;
        this.metronome = metronome;
        this.clockwork = new Clockwork();
        this.metronome.attachTo(this.clockwork);
        this.clockwork.attachChime(chime);
        this.clockwork.attachBell(new Bell() {
            public void ring(int ticks) {
                Clock.this.adjust();
            }
        }, JttTime.TICKS_PER_INTERVAL);
    }

    public void adjust() {
        astrolabe.updateLocation();
        DayInterval interval = astrolabe.getCurrentInterval();
		long tickLength = interval.getLength() / JttTime.TICKS_PER_INTERVAL;
        metronome.start(interval.getStart(), tickLength);
    }

    public void addClockEvent(ClockEvent event) {
        clockwork.attachBell(new ClockEventBell(event), event.getGranularity());
    }

    private static class ClockEventBell implements Bell {
        private final ClockEvent event;

        public ClockEventBell(ClockEvent event) {
            this.event = event;
        }

        public void ring(int ticks) {
            event.trigger(ticks);
        }
    }
}
