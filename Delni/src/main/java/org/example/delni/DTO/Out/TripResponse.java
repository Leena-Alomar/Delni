package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private Integer id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysCount;
    private Integer groupSize;
    private String tripType;
    private String budgetTier;
    private Double totalCostEstimate;
    private Boolean includeTikTokTrending;
    private Boolean weatherAware;
    private Boolean includePrayerSchedule;
    private Boolean includeTopRatings;
    private String userPrompt;
    private String aiItineraryLogic;
    private LocalDateTime createdAt;
    private Integer userId;
    private Integer cityId;
    private String cityName;
    private Integer tripPlacesCount;
}
