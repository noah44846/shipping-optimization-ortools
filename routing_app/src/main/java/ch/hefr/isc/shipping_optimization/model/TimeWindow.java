package ch.hefr.isc.shipping_optimization.model;

import java.time.LocalTime;

public record TimeWindow(LocalTime start, LocalTime end) {
    public static TimeWindow of(int startHour, int startMinute, int endHour, int endMinute) {
        return new TimeWindow(LocalTime.of(startHour, startMinute), LocalTime.of(endHour, endMinute));
    }

    public boolean contains(LocalTime time) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    public int startAsMinutes() {
        return start.getHour() * 60 + start.getMinute();
    }

    public int endAsMinutes() {
        return end.getHour() * 60 + end.getMinute();
    }
}
