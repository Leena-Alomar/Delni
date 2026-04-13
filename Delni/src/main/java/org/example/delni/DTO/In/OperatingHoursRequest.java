package org.example.delni.DTO.In;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalTime;

@Data
public class OperatingHoursRequest {

    @Min(value = 0, message = "dayOfWeek must be between 0 and 6")
    @Max(value = 6, message = "dayOfWeek must be between 0 and 6")
    private Integer dayOfWeek;

    private LocalTime openTime;
    private LocalTime closeTime;
    private Boolean splitShift;
    private LocalTime secondOpen;
    private LocalTime secondClose;
    private Boolean prayerBreak;
    private String notes;
}
