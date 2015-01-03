package com.aragaer.jtt.astronomy;
// vim: et ts=4 sts=4 sw=4

import org.junit.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import com.aragaer.jtt.clockwork.TestClock;
import com.aragaer.jtt.clockwork.TestModule;
import com.aragaer.jtt.location.Location;
import com.aragaer.jtt.test.*;


public class DayIntervalServiceTest {

    private DayIntervalService astrolabe;
    private TestCalculator calculator;
    private TestClock clock;

    @Before
    public void setup() {
        TestModule module = new TestModule();
        clock = new TestClock(module.getChime(), module.getMetronome());
        calculator = (TestCalculator) module.getCalculator();
        astrolabe = new DayIntervalService(calculator);
        clock.bindToDayIntervalService(astrolabe);
    }

    // TODO: Remove
    @Test
    public void shouldReturnCalculatorResult() {
        DayInterval interval = DayInterval.Day(10000, 20000);
        calculator.setNextResult(interval);

        assertThat(astrolabe.getCurrentInterval(), equalTo(interval));
    }

    @Test public void shouldNotifyClockOnDateTimeChange() {
        DayInterval interval = DayInterval.Day(10000, 20000);
        calculator.setNextResult(interval);
        long before = System.currentTimeMillis();

        astrolabe.onDateTimeChanged();

        long after = System.currentTimeMillis();
        assertThat(clock.currentInterval, equalTo(interval));
        assertThat(calculator.timestamp, greaterThanOrEqualTo(before));
        assertThat(calculator.timestamp, lessThanOrEqualTo(after));
    }

    @Test public void shouldNotifyClockOnLocationChange() {
        DayInterval interval = DayInterval.Day(10000, 20000);
        calculator.setNextResult(interval);
        Location location = new Location(4, 5);
        long before = System.currentTimeMillis();

        astrolabe.onLocationChanged(location);

        long after = System.currentTimeMillis();
        assertThat(calculator.location, equalTo(location));
        assertThat(clock.currentInterval, equalTo(interval));
        assertThat(calculator.timestamp, greaterThanOrEqualTo(before));
        assertThat(calculator.timestamp, lessThanOrEqualTo(after));
    }

    @Test public void shouldCalculateNewIntervalOnIntervalEnd() {
        DayInterval interval = DayInterval.Day(10000, 20000);
        calculator.setNextResult(interval);
        long before = System.currentTimeMillis();

        astrolabe.onIntervalEnded();

        long after = System.currentTimeMillis();
        assertThat(clock.currentInterval, equalTo(interval));
        assertThat(calculator.timestamp, greaterThanOrEqualTo(before));
        assertThat(calculator.timestamp, lessThanOrEqualTo(after));
    }
}
