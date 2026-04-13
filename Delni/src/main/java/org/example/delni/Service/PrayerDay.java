package org.example.delni.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PrayerDay {
    private LocalDate date;
    private LocalTime fajr;
    private LocalTime sunrise;
    private LocalTime dhuhr;
    private LocalTime asr;
    private LocalTime maghrib;
    private LocalTime isha;
    private String methodName;

    public LocalTime recommendedTimeForSlot(String timeSlot) {
        String normalized = timeSlot == null ? "" : timeSlot.trim().toLowerCase();

        return switch (normalized) {
            case "morning" -> clampTime(
                    sunrise != null ? sunrise.plusMinutes(120) : LocalTime.of(9, 0),
                    LocalTime.of(8, 30),
                    LocalTime.of(10, 30));
            case "evening" -> clampTime(
                    maghrib != null ? maghrib.plusMinutes(90) : LocalTime.of(19, 0),
                    LocalTime.of(18, 30),
                    LocalTime.of(21, 0));
            default -> clampTime(
                    dhuhr != null ? dhuhr.plusMinutes(60) : LocalTime.of(14, 0),
                    LocalTime.of(13, 0),
                    LocalTime.of(15, 0));
        };
    }

    public boolean isNearPrayerWindow(LocalTime time) {
        return isNear(time, fajr) || isNear(time, dhuhr) || isNear(time, asr) || isNear(time, maghrib) || isNear(time, isha);
    }

    public String nearbyPrayerLabel(LocalTime time) {
        if (time == null) {
            return null;
        }

        Map<String, LocalTime> prayers = new LinkedHashMap<>();
        prayers.put("Fajr", fajr);
        prayers.put("Dhuhr", dhuhr);
        prayers.put("Asr", asr);
        prayers.put("Maghrib", maghrib);
        prayers.put("Isha", isha);

        String nearestPrayer = null;
        int nearestDifference = Integer.MAX_VALUE;

        for (Map.Entry<String, LocalTime> entry : prayers.entrySet()) {
            if (!isNear(time, entry.getValue())) {
                continue;
            }

            int difference = Math.abs(time.toSecondOfDay() - entry.getValue().toSecondOfDay());
            if (difference < nearestDifference) {
                nearestDifference = difference;
                nearestPrayer = entry.getKey();
            }
        }

        return nearestPrayer;
    }

    private boolean isNear(LocalTime candidate, LocalTime prayerTime) {
        if (candidate == null || prayerTime == null) {
            return false;
        }

        int difference = Math.abs(candidate.toSecondOfDay() - prayerTime.toSecondOfDay());
        return difference <= 30 * 60;
    }

    private LocalTime clampTime(LocalTime candidate, LocalTime min, LocalTime max) {
        if (candidate == null) {
            return min;
        }

        if (candidate.isBefore(min)) {
            return min;
        }

        if (candidate.isAfter(max)) {
            return max;
        }

        return candidate;
    }
}
