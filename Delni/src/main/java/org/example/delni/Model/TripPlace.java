package org.example.delni.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class TripPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "INT NOT NULL")
    @NotNull(message = "Day number is required")
    private Integer dayNumber;

    @Column(columnDefinition = "INT NOT NULL")
    @NotNull(message = "Order in day is required")
    private Integer orderInDay;

    @Column(columnDefinition = "VARCHAR(20)")
    private String timeSlot;

    @Column(columnDefinition = "INT")
    private Integer travelTimeMins;

    @Column(columnDefinition = "INT")
    private Integer activityDuration;

    @Column(columnDefinition = "TEXT")
    private String aiNote;

    // Relationships

    @ManyToOne
    @JsonIgnore
    private Trip trip;

    @ManyToOne
    private Place place;
}
