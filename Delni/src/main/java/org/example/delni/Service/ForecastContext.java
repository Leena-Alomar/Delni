package org.example.delni.Service;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ForecastContext {

    private final boolean available;
    private final Map<LocalDate, WeatherDay> dailyForecast;
    private final Map<LocalDate, List<WeatherHour>> hourlyForecast;

    public ForecastContext(boolean available, Map<LocalDate, WeatherDay> dailyForecast, Map<LocalDate, List<WeatherHour>> hourlyForecast) {
        this.available = available;
        this.dailyForecast = dailyForecast == null ? new LinkedHashMap<>() : dailyForecast;
        this.hourlyForecast = hourlyForecast == null ? new LinkedHashMap<>() : hourlyForecast;
    }

    public static ForecastContext empty() {
        return new ForecastContext(false, new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public WeatherDay getDay(LocalDate date) {
        return dailyForecast.get(date);
    }

    public WeatherHour getHour(LocalDate date, LocalTime time) {
        List<WeatherHour> hours = hourlyForecast.get(date);
        if (hours == null || hours.isEmpty() || time == null) {
            return null;
        }

        return hours.stream()
                .min(Comparator.comparingLong(hour ->
                        Math.abs(hour.getDateTime().toLocalTime().toSecondOfDay() - time.toSecondOfDay())))
                .orElse(null);
    }
}
