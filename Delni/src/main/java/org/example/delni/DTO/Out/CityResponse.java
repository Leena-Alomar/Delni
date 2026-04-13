package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityResponse {

    private Integer id;
    private String name;
    private String region;
    private Double latitude;
    private Double longitude;
    private String description;
}
