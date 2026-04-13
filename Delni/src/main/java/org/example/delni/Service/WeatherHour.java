package org.example.delni.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WeatherHour {
    private LocalDateTime dateTime;
    private Double temperature;
    private Double apparentTemperature;
    private Integer precipitationProbability;
    private Integer weatherCode;
    private String summary;
    private Boolean outdoorFriendly;
    private Boolean harshWeather;
}
