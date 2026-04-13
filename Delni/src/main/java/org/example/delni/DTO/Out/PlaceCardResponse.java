package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceCardResponse {

    private Integer id;
    private String name;
    private String description;
    private String category;
    private String vibeTag;
    private String cityName;
    private Double googleRating;
    private Double tiktokTrendScore;
    private Double smartScore;
    private Boolean trending;
    private String trendReason;
    private String imageUrl;
    private String googleMapsUrl;
    private Double latitude;
    private Double longitude;
    private Integer operatingHoursCount;
    private Integer mediaCount;
    private String subtitle;
    private String scoreLabel;
    private String trendLabel;
    private List<String> badges;
    private String primaryActionUrl;
}
