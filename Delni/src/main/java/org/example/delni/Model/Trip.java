package org.example.delni.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "VARCHAR(50) NOT NULL")
    @NotEmpty(message = "Title cannot be empty")
    @Size(min = 4, max = 50, message = "Title must be between 4 and 50 characters")
    private String title;

    @Column(columnDefinition = "DATETIME NOT NULL")
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;

    @Column(columnDefinition = "DATETIME NOT NULL")
    @NotNull(message = "End date is required")
    @FutureOrPresent(message = "End date cannot be in the past")
    private LocalDate endDate;


    @NotNull(message = "Days count is required")
    @Min(value = 1, message = "Trip must be at least 1 day")
    private Integer daysCount;

    @NotNull(message = "Group size is required")
    @Min(value = 1, message = "Group size must be at least 1")
    private Integer groupSize;

    @NotEmpty(message = "Trip type is required")
    @Pattern(regexp = "Family|Friends|Solo",
            message = "Trip type must be Family, Friends, or Solo")
    private String tripType;

    @NotEmpty(message = "Budget tier is required")
    @Pattern(regexp = "Low|Medium|High",
            message = "Budget tier must be Low, Medium, or High")
    private String budgetTier;

    @PositiveOrZero(message = "Total cost cannot be negative")
    private Double totalCostEstimate;

    private Boolean includeTikTok;

    private Boolean weatherAware;

    private Boolean includePrayerBreak;

    private Boolean includeTopRatings;

    @Column(columnDefinition = "TEXT")
    private String userPrompt;

    @Column(columnDefinition = "TEXT")
    private String aiItineraryLogic;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Relationships

    @ManyToOne
    @JsonIgnore
    private User user;

    @ManyToOne
    @JsonIgnore
    private City city;

}
