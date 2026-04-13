package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoritePlaceResponse {

    private Integer favoriteId;
    private LocalDateTime savedAt;
    private Integer placeId;
    private String name;
    private String category;
    private String vibeTag;
    private String cityName;
    private Double googleRating;
    private Boolean trending;
    private String imageUrl;
    private String description;
    private String googleMapsUrl;
    private String subtitle;
    private String scoreLabel;
    private String trendLabel;
    private List<String> badges;
}
