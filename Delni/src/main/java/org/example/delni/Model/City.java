package org.example.delni.Model;

import jakarta.persistence.*;
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

    private String name;

    private String region;

    private Double latitude;

    private Double longitude;

    private String text;

    // Relationships

    @OneToMany(mappedBy = "trip")
    private Set<Trip> trip;
}
