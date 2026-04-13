package org.example.delni.Service;

import lombok.Getter;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class PrayerContext {

    private final boolean available;
    private final Map<LocalDate, PrayerDay> schedules;

    public PrayerContext(boolean available, Map<LocalDate, PrayerDay> schedules) {
        this.available = available;
        this.schedules = schedules == null ? new LinkedHashMap<>() : schedules;
    }

    public static PrayerContext empty() {
        return new PrayerContext(false, new LinkedHashMap<>());
    }

    public PrayerDay getDay(LocalDate date) {
        return schedules.get(date);
    }
}
