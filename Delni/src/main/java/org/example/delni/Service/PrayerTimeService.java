package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.DTO.External.PrayerTimesApiResponse;
import org.example.delni.DTO.External.PrayerTimings;
import org.example.delni.Model.City;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PrayerTimeService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Value("${prayer.api.url:https://api.aladhan.com/v1/timings}")
    private String prayerApiUrl;

    @Value("${prayer.api.method:4}")
    private Integer calculationMethod;

    private final RestTemplate restTemplate;
    private final Map<String, PrayerContext> prayerCache = new ConcurrentHashMap<>();

    public PrayerContext fetchPrayerTimes(City city, LocalDate startDate, LocalDate endDate) {
        if (city == null || city.getLatitude() == null || city.getLongitude() == null || startDate == null || endDate == null) {
            return PrayerContext.empty();
        }

        String cacheKey = buildCacheKey(city, startDate, endDate);
        PrayerContext cachedContext = prayerCache.get(cacheKey);
        if (cachedContext != null) {
            return cachedContext;
        }

        Map<LocalDate, PrayerDay> schedules = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            try {
                String url = UriComponentsBuilder.fromUriString(prayerApiUrl + "/" + DATE_FORMATTER.format(date))
                        .queryParam("latitude", city.getLatitude())
                        .queryParam("longitude", city.getLongitude())
                        .queryParam("method", calculationMethod)
                        .build(true)
                        .toUriString();

                PrayerTimesApiResponse response = restTemplate.getForObject(url, PrayerTimesApiResponse.class);
                PrayerDay prayerDay = mapPrayerDay(date, response);
                if (prayerDay != null) {
                    schedules.put(date, prayerDay);
                }
            } catch (Exception ignored) {
                // Degrade gracefully. Trip generation can still work with operating hours if the API is unavailable.
            }
        }

        PrayerContext context = schedules.isEmpty()
                ? PrayerContext.empty()
                : new PrayerContext(true, schedules);
        prayerCache.put(cacheKey, context);
        return context;
    }

    private String buildCacheKey(City city, LocalDate startDate, LocalDate endDate) {
        return city.getId() + "|" + city.getLatitude() + "|" + city.getLongitude()
                + "|" + startDate + "|" + endDate + "|" + calculationMethod;
    }

    private PrayerDay mapPrayerDay(LocalDate date, PrayerTimesApiResponse response) {
        if (response == null || response.getData() == null || response.getData().getTimings() == null) {
            return null;
        }

        PrayerTimings timings = response.getData().getTimings();
        String methodName = response.getData().getMeta() != null && response.getData().getMeta().getMethod() != null
                ? response.getData().getMeta().getMethod().getName()
                : "Prayer API";

        return new PrayerDay(
                date,
                parseTime(timings.getFajr()),
                parseTime(timings.getSunrise()),
                parseTime(timings.getDhuhr()),
                parseTime(timings.getAsr()),
                parseTime(timings.getMaghrib()),
                parseTime(timings.getIsha()),
                methodName
        );
    }

    private LocalTime parseTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return null;
        }

        String cleaned = rawTime.trim();
        if (cleaned.length() >= 5) {
            cleaned = cleaned.substring(0, 5);
        }

        return LocalTime.parse(cleaned);
    }

}
