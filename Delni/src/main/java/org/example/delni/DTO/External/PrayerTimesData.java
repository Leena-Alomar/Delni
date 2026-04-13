package org.example.delni.DTO.External;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrayerTimesData {

    private PrayerTimings timings;
    private PrayerMeta meta;
}
