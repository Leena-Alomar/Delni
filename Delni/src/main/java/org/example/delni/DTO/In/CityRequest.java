package org.example.delni.DTO.In;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CityRequest {

    @NotEmpty(message = "City name cannot be empty")
    @Size(min = 2, max = 50, message = "City name must be at least 2 characters and less than 50 characters")
    private String name;

    @NotEmpty(message = "Region cannot be empty")
    @Size(min = 2, max = 50, message = "Region must be at least 2 characters and less than 50 characters")
    private String region;

    @NotNull(message = "Latitude cannot be null")
    private Double latitude;

    @NotNull(message = "Longitude cannot be null")
    private Double longitude;

    private String description;
}
