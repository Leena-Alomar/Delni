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
public class TripRequest {

    @NotNull(message = "Title is required")
    @Size(min = 4, max = 50, message = "Title must be between 4 and 50 characters")
    private String title;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @FutureOrPresent(message = "End date cannot be in the past")
    private LocalDate endDate;

    @NotNull(message = "Days count is required")
    @Min(value = 1, message = "Trip must be at least 1 day")
    private Integer daysCount;

    @NotNull(message = "Group size is required")
    @Min(value = 1, message = "Group size must be at least 1")
    private Integer groupSize;

    @NotNull(message = "Trip type is required")
    @Pattern(regexp = "Family|Friends|Solo|Couple",
            message = "Trip type must be Family, Friends, Solo, or Couple")
    private String tripType;

    @NotNull(message = "Budget tier is required")
    @Pattern(regexp = "Low|Medium|High",
            message = "Budget tier must be Low, Medium, or High")
    private String budgetTier;

    @PositiveOrZero(message = "Total cost cannot be negative")
    private Double totalCostEstimate;

    private Boolean includeTikTokTrending;
    private Boolean weatherAware;
    private Boolean includePrayerSchedule;
    private Boolean includeTopRatings;
    private String userPrompt;

    @NotNull(message = "User is required")
    private Integer userId;

    @NotNull(message = "City is required")
    private Integer cityId;
}
