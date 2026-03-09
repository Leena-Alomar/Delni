package org.example.delni.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class PlaceMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "VARCHAR(20)")
    @NotEmpty(message = "Media type is required")
    @Pattern(regexp = "tiktok_video|image",
             message = "Media type must be either 'tiktok_video' or 'image'")
    private String mediaType;

    @Column(columnDefinition = "VARCHAR(255) NOT NULL")
    @NotEmpty(message = "URL cannot be empty")
    private String url;

    @Column(columnDefinition = "VARCHAR(255)")
    private String thumbnail;

    @Column(columnDefinition = "VARCHAR(255)")
    private String caption;

    // Relationships

    @ManyToOne
    @JsonIgnore
    private Place place;
}