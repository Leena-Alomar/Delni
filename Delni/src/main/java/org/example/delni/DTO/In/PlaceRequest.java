package org.example.delni.DTO.In;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class PlaceRequest {

    @NotEmpty(message = "Name cannot be empty")
    private String name;

    private String description;
    private String category;
    private String vibeTag;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;

    private Double googleRating;
    private String googleMapsUrl;
    private String imageUrl;
    private String tiktokVideoUrl;
    private String trendReason;

    @NotNull(message = "City is required")
    private Integer cityId;

    @Valid
    private List<OperatingHoursRequest> operatingHours;

    @Valid
    private List<PlaceMediaRequest> mediaList;
}
