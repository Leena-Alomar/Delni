package org.example.delni.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class OperatingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer dayOfWeek; // 0=Sunday...6=Saturday

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