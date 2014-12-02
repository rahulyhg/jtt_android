package com.aragaer.jtt.clockwork;
// vim: et ts=4 sts=4 sw=4

import org.junit.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.aragaer.jtt.astronomy.DayInterval;
import com.aragaer.jtt.core.JttTime;


public class ClockTest {

    private Clock clock;
    private TestMetronome metronome;
    private TestAstrolabe astrolabe;
    private TestChime chime;

    @Before
    public void setUp() {
        metronome = new TestMetronome();
        astrolabe = new TestAstrolabe();
        chime = new TestChime();
        clock = new Clock(astrolabe, chime, metronome);
    }

    @Test
    public void shouldTriggerEvent() {
        TestEvent event = new TestEvent();
        clock.addClockEvent(event);
        metronome.tick(42);
        assertThat(event.lastTriggeredAt, equalTo(42));
        assertThat("chime ding number", chime.getLastTick(), equalTo(42));
    }

    @Test
    public void shouldTriggerEventWithGranularity() {
        TestEvent event = new TestEvent(20);
        clock.addClockEvent(event);
        metronome.tick(42);
        assertThat(event.lastTriggeredAt, equalTo(40));
    }

    @Test
    public void shouldTriggerMultipleEvents() {
        TestEvent event1 = new TestEvent(20);
        TestEvent event2 = new TestEvent(1);
        clock.addClockEvent(event1);
        clock.addClockEvent(event2);
        metronome.tick(42);
        assertThat(event1.lastTriggeredAt, equalTo(40));
        assertThat(event2.lastTriggeredAt, equalTo(42));
    }

    @Test
    public void shouldUpdateLocationWhenAdjusted() {
        astrolabe.setNextResult(DayInterval.Day(0, 1));
        assertThat(astrolabe.updateLocationCalls, equalTo(0));
        clock.adjust();
        assertThat(astrolabe.updateLocationCalls, equalTo(1));
    }

    @Test
    public void shouldStartMetronomeBasedOnAstrolabeResult() {
        astrolabe.setNextResult(DayInterval.Day(10, 10 + JttTime.TICKS_PER_INTERVAL * 5));

        clock.adjust();

        assertThat(metronome.start, equalTo(10L));
        assertThat(metronome.tickLength, equalTo(5L));
    }

    @Test
    public void shouldReAdjustWhenIntervalEnds() {
        astrolabe.setNextResult(DayInterval.Day(10, 10 + JttTime.TICKS_PER_INTERVAL * 5));

        metronome.tick(JttTime.TICKS_PER_INTERVAL + 5);

        assertThat(metronome.start, equalTo(10L));
        assertThat(metronome.tickLength, equalTo(5L));
    }

    private static class TestEvent implements ClockEvent {
        int lastTriggeredAt;
        final int granularity;

        public TestEvent() {
            this(1);
        }

        public TestEvent(int granularity) {
            this.granularity = granularity;
        }

        public void trigger(int ticks) {
            lastTriggeredAt = ticks;
        }

        public int getGranularity() {
            return granularity;
        }
    }
}
