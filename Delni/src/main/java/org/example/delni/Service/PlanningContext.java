package org.example.delni.Service;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class PlanningContext {

    private final ForecastContext weatherForecast;
    private final PrayerContext prayerContext;

    public PlanningContext(ForecastContext weatherForecast, PrayerContext prayerContext) {
        this.weatherForecast = weatherForecast == null ? ForecastContext.empty() : weatherForecast;
        this.prayerContext = prayerContext == null ? PrayerContext.empty() : prayerContext;
    }

    public WeatherDay getWeatherDay(LocalDate date) {
        return weatherForecast.getDay(date);
    }

    public WeatherHour getWeatherHour(LocalDate date, LocalTime time) {
        return weatherForecast.getHour(date, time);
    }

    public PrayerDay getPrayerDay(LocalDate date) {
        return prayerContext.getDay(date);
    }
}
