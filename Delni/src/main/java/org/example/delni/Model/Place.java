package org.example.delni.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
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
    private String category;

    @Column(columnDefinition = "VARCHAR(50)")
    private String vibeTag;

    @Column(columnDefinition = "DOUBLE NOT NULL")
    @NotNull(message = "Latitude is required")
    private Double latitude;

    @Column(columnDefinition = "DOUBLE NOT NULL")
    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Double googleRating;

    @Column(columnDefinition = "VARCHAR(255)")
    private String googleMapsUrl;

    @Column(columnDefinition = "VARCHAR(100)")
    private String googlePlaceId;

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
    @NotNull(message = "City is required")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private City city;

    @Valid
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperatingHours> operatingHours;

    @Valid
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaceMedia> mediaList;

    @OneToMany(mappedBy = "place")
    @JsonIgnore
    private List<TripPlace> tripPlaces;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Favorite> favorites;
}
