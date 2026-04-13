package org.example.delni.DTO.In;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TripGenerationRequest {

    @Size(max = 50)
    private String title;

    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull
    @FutureOrPresent
    private LocalDate endDate;

    @Min(1)
    private Integer daysCount;

    @Min(1)
    private Integer groupSize;

    @Pattern(regexp = "Family|Friends|Solo|Couple", message = "Trip type must be Family, Friends, Solo, or Couple")
    private String tripType;

    @Pattern(regexp = "Low|Medium|High|Economy|Luxury", message = "Budget tier must be Low, Medium, High, Economy, or Luxury")
    private String budgetTier;

    @PositiveOrZero
    private Double totalCostEstimate;
    private Boolean includeTikTokTrending;
    private Boolean weatherAware;
    private Boolean includePrayerSchedule;
    private Boolean includeTopRatings;

    @Size(max = 2000)
    private String userPrompt;

    @NotNull
    private Integer userId;

    @NotNull
    private Integer cityId;
    private String category;
    private String vibeTag;
}
