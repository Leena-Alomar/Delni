package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.DTO.External.WeatherApiResponse;
import org.example.delni.Model.City;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class WeatherService {

    @Value("${weather.api.url:https://api.open-meteo.com/v1/forecast}")
    private String weatherApiUrl;

    @Value("${weather.api.timezone:Asia/Riyadh}")
    private String weatherTimezone;

    private final RestTemplate restTemplate;
    private final Map<String, ForecastContext> forecastCache = new ConcurrentHashMap<>();

    public ForecastContext fetchForecast(City city, LocalDate startDate, LocalDate endDate) {
        if (city == null || city.getLatitude() == null || city.getLongitude() == null || startDate == null || endDate == null) {
            return ForecastContext.empty();
        }

        String cacheKey = buildCacheKey(city, startDate, endDate);
        ForecastContext cachedContext = forecastCache.get(cacheKey);
        if (cachedContext != null) {
            return cachedContext;
        }

        try {
            String url = UriComponentsBuilder.fromUriString(weatherApiUrl)
                    .queryParam("latitude", city.getLatitude())
                    .queryParam("longitude", city.getLongitude())
                    .queryParam("timezone", weatherTimezone)
                    .queryParam("start_date", startDate)
                    .queryParam("end_date", endDate)
                    .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max")
                    .queryParam("hourly", "temperature_2m,apparent_temperature,precipitation_probability,weather_code")
                    .build(true)
                    .toUriString();

            WeatherApiResponse response = restTemplate.getForObject(url, WeatherApiResponse.class);
            if (response == null) {
                return ForecastContext.empty();
            }

            ForecastContext forecastContext = mapForecast(response);
            forecastCache.put(cacheKey, forecastContext);
            return forecastContext;
        } catch (Exception exception) {
            return ForecastContext.empty();
        }
    }

    private String buildCacheKey(City city, LocalDate startDate, LocalDate endDate) {
        return city.getId() + "|" + city.getLatitude() + "|" + city.getLongitude()
                + "|" + startDate + "|" + endDate + "|" + weatherTimezone;
    }

    private ForecastContext mapForecast(WeatherApiResponse response) {
        Map<LocalDate, WeatherDay> dailyForecast = new LinkedHashMap<>();
        Map<LocalDate, List<WeatherHour>> hourlyForecast = new LinkedHashMap<>();

        if (response.getDaily() != null && response.getDaily().getTime() != null) {
            for (int index = 0; index < response.getDaily().getTime().size(); index++) {
                LocalDate date = LocalDate.parse(response.getDaily().getTime().get(index));
                Double maxTemperature = getValue(response.getDaily().getTemperature2mMax(), index);
                Double minTemperature = getValue(response.getDaily().getTemperature2mMin(), index);
                Integer precipitationProbability = getValue(response.getDaily().getPrecipitationProbabilityMax(), index);
                Integer weatherCode = getValue(response.getDaily().getWeatherCode(), index);

                dailyForecast.put(date, new WeatherDay(
                        date,
                        maxTemperature,
                        minTemperature,
                        precipitationProbability,
                        weatherCode,
                        summarizeWeatherCode(weatherCode),
                        isOutdoorFriendly(maxTemperature, precipitationProbability, weatherCode),
                        isHarshWeather(maxTemperature, precipitationProbability, weatherCode)
                ));
            }
        }

        if (response.getHourly() != null && response.getHourly().getTime() != null) {
            for (int index = 0; index < response.getHourly().getTime().size(); index++) {
                LocalDateTime dateTime = LocalDateTime.parse(response.getHourly().getTime().get(index));
                Double temperature = getValue(response.getHourly().getTemperature2m(), index);
                Double apparentTemperature = getValue(response.getHourly().getApparentTemperature(), index);
                Integer precipitationProbability = getValue(response.getHourly().getPrecipitationProbability(), index);
                Integer weatherCode = getValue(response.getHourly().getWeatherCode(), index);

                hourlyForecast.computeIfAbsent(dateTime.toLocalDate(), ignored -> new ArrayList<>())
                        .add(new WeatherHour(
                                dateTime,
                                temperature,
                                apparentTemperature,
                                precipitationProbability,
                                weatherCode,
                                summarizeWeatherCode(weatherCode),
                                isOutdoorFriendly(apparentTemperature != null ? apparentTemperature : temperature, precipitationProbability, weatherCode),
                                isHarshWeather(apparentTemperature != null ? apparentTemperature : temperature, precipitationProbability, weatherCode)
                        ));
            }
        }

        return new ForecastContext(true, dailyForecast, hourlyForecast);
    }

    private boolean isOutdoorFriendly(Double temperature, Integer precipitationProbability, Integer weatherCode) {
        double normalizedTemperature = temperature == null ? 28.0 : temperature;
        int normalizedPrecipitation = precipitationProbability == null ? 0 : precipitationProbability;
        return normalizedPrecipitation < 35 && normalizedTemperature >= 18 && normalizedTemperature <= 32 && !isSevereCode(weatherCode);
    }

    private boolean isHarshWeather(Double temperature, Integer precipitationProbability, Integer weatherCode) {
        double normalizedTemperature = temperature == null ? 28.0 : temperature;
        int normalizedPrecipitation = precipitationProbability == null ? 0 : precipitationProbability;
        return normalizedTemperature >= 36 || normalizedTemperature <= 10 || normalizedPrecipitation >= 50 || isSevereCode(weatherCode);
    }

    private boolean isSevereCode(Integer weatherCode) {
        if (weatherCode == null) {
            return false;
        }

        return weatherCode >= 51 || weatherCode == 45 || weatherCode == 48;
    }

    private String summarizeWeatherCode(Integer weatherCode) {
        if (weatherCode == null) {
            return "Forecast unavailable";
        }

        return switch (weatherCode) {
            case 0 -> "Clear sky";
            case 1, 2, 3 -> "Partly cloudy";
            case 45, 48 -> "Foggy";
            case 51, 53, 55, 56, 57 -> "Drizzle";
            case 61, 63, 65, 66, 67, 80, 81, 82 -> "Rain expected";
            case 71, 73, 75, 77, 85, 86 -> "Snow expected";
            case 95, 96, 99 -> "Thunderstorm risk";
            default -> "Mixed conditions";
        };
    }

    private <T> T getValue(List<T> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }

        return values.get(index);
    }

}
