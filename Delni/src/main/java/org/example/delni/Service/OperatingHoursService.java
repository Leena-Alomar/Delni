package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.Model.OperatingHours;
import org.example.delni.Model.Place;
import org.example.delni.Repository.OperatingHoursRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperatingHoursService {

    private final OperatingHoursRepository operatingHoursRepository;

    public List<OperatingHours> getOrderedByPlaceId(Integer placeId) {
        return new ArrayList<>(operatingHoursRepository.findAllByPlaceIdOrderByDayOfWeekAsc(placeId));
    }

    public List<OperatingHours> normalizeForPlace(Place place) {
        List<OperatingHours> normalizedHours = place.getOperatingHours() == null
                ? new ArrayList<>()
                : new ArrayList<>(place.getOperatingHours());

        for (OperatingHours hours : normalizedHours) {
            hours.setPlace(place);
            if (hours.getIsSplitShift() == null) {
                hours.setIsSplitShift(false);
            }
            if (hours.getPrayerBreak() == null) {
                hours.setPrayerBreak(false);
            }
        }

        normalizedHours.sort(Comparator.comparing(OperatingHours::getDayOfWeek, Comparator.nullsLast(Integer::compareTo)));
        return normalizedHours;
    }

    public boolean isOpenAt(Place place, LocalDate date, LocalTime candidateTime, boolean prayerBreakActive) {
        List<OperatingHours> operatingHours = place.getOperatingHours();
        if (operatingHours == null || operatingHours.isEmpty()) {
            return true;
        }

        int dayOfWeek = toSchemaDayOfWeek(date.getDayOfWeek());
        return operatingHours.stream()
                .filter(hours -> hours.getDayOfWeek() != null && hours.getDayOfWeek().equals(dayOfWeek))
                .anyMatch(hours -> isOpenInWindow(hours, candidateTime, prayerBreakActive));
    }

    private boolean isOpenInWindow(OperatingHours hours, LocalTime candidateTime, boolean prayerBreakActive) {
        if (Boolean.TRUE.equals(hours.getPrayerBreak()) && prayerBreakActive) {
            return false;
        }

        boolean firstWindowOpen = isWithinWindow(candidateTime, hours.getOpenTime(), hours.getCloseTime());
        boolean secondWindowOpen = Boolean.TRUE.equals(hours.getIsSplitShift())
                && isWithinWindow(candidateTime, hours.getSecondOpen(), hours.getSecondClose());

        return firstWindowOpen || secondWindowOpen;
    }

    private boolean isWithinWindow(LocalTime candidateTime, LocalTime openTime, LocalTime closeTime) {
        if (candidateTime == null || openTime == null || closeTime == null) {
            return false;
        }

        if (openTime.equals(closeTime)) {
            return true;
        }

        if (closeTime.isBefore(openTime)) {
            return !candidateTime.isBefore(openTime) || !candidateTime.isAfter(closeTime);
        }

        return !candidateTime.isBefore(openTime) && !candidateTime.isAfter(closeTime);
    }

    private int toSchemaDayOfWeek(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case SUNDAY -> 0;
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
        };
    }
}
