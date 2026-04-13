package org.example.delni.DTO.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrayerTimings {

    @JsonProperty("Fajr")
    private String fajr;

    @JsonProperty("Sunrise")
    private String sunrise;

    @JsonProperty("Dhuhr")
    private String dhuhr;

    @JsonProperty("Asr")
    private String asr;

    @JsonProperty("Maghrib")
    private String maghrib;

    @JsonProperty("Isha")
    private String isha;
}
