package org.example.delni.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDay {
    private LocalDate date;
    private Double maxTemperature;
    private Double minTemperature;
    private Integer precipitationProbability;
    private Integer weatherCode;
    private String summary;
    private Boolean outdoorFriendly;
    private Boolean harshWeather;
}
