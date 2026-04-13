package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteResponse {

    private Integer favoriteId;
    private LocalDateTime savedAt;
    private Integer userId;
    private Integer placeId;
    private String placeName;
    private String category;
    private String cityName;
    private Double googleRating;
    private Boolean trending;
    private String imageUrl;
}
