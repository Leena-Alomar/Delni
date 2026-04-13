package org.example.delni.DTO.External;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WeatherApiResponse {

    private Hourly hourly;
    private Daily daily;

    @Data
    public static class Hourly {
        private List<String> time;

        @JsonProperty("temperature_2m")
        private List<Double> temperature2m;

        @JsonProperty("apparent_temperature")
        private List<Double> apparentTemperature;

        @JsonProperty("precipitation_probability")
        private List<Integer> precipitationProbability;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;
    }

    @Data
    public static class Daily {
        private List<String> time;

        @JsonProperty("temperature_2m_max")
        private List<Double> temperature2mMax;

        @JsonProperty("temperature_2m_min")
        private List<Double> temperature2mMin;

        @JsonProperty("precipitation_probability_max")
        private List<Integer> precipitationProbabilityMax;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;
    }
}
