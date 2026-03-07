package org.example.delni.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "VARCHAR(50) NOT NULL")
    @NotEmpty(message = "City name cannot be empty")
    @Size(min = 2, max = 50, message = "City name must be at least 2 characters and less than 50 characters")
    private String name;

    @Column(columnDefinition = "VARCHAR(50) NOT NULL")
    @NotEmpty(message = "Region cannot be empty")
    @Size(min = 2, max = 50, message = "Region must be at least 2 characters and less than 50 characters")
    private String region;

    @Column(columnDefinition = "DOUBLE NOT NULL")
    @NotNull(message = "Latitude cannot be null")
    private Double latitude;

    @Column(columnDefinition = "DOUBLE NOT NULL")
    @NotNull(message = "Longitude cannot be null")
    private Double longitude;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Relationships

    @OneToMany(mappedBy = "city", cascade = CascadeType.ALL)
    private Set<Trip> trips;
}
