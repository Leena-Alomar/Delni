package org.example.delni.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class OperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    @Min(value = 0, message = "dayOfWeek must be between 0 and 6")
    @Max(value = 6, message = "dayOfWeek must be between 0 and 6")
    private Integer dayOfWeek;

    private LocalTime openTime;
    private LocalTime closeTime;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isSplitShift;

    private LocalTime secondOpen;
    private LocalTime secondClose;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean prayerBreak;

    @Column(columnDefinition = "VARCHAR(255)")
    private String notes;

    // Relationships

    @ManyToOne
    @JsonIgnore
    private Place place;
}
