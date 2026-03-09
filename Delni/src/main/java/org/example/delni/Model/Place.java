package org.example.delni.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    @NotEmpty(message = "Name cannot be empty")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "VARCHAR(50)")
    private String category; // 'nature', 'history', 'cafe', etc.

    @Column(columnDefinition = "VARCHAR(50)")
    private String vibeTag; // 'historical', 'modern', 'nature'

    @Column(columnDefinition = "DOUBLE NOT NULL")
    @NotNull(message = "Latitude is required")
    private Double latitude;

    @Column(columnDefinition = "DOUBLE NOT NULL")
    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Double googleRating;

    @Column(columnDefinition = "VARCHAR(255)")
    private String googleMapsUrl;

    private Double tiktokTrendScore;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isTrending;

    private Double smartScore;

    @Column(columnDefinition = "VARCHAR(255)")
    private String imageUrl;

    @Column(columnDefinition = "VARCHAR(255)")
    private String tiktokVideoUrl;

    @Column(columnDefinition = "TEXT")
    private String trendReason;


    // Relationships

    @ManyToOne
    @JsonIgnore
    private City city;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL)
    private List<OperatingHours> operatingHours;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL)
    private List<PlaceMedia> mediaList;

    @OneToMany(mappedBy = "place")
    @JsonIgnore
    private List<TripPlace> tripPlaces;
}