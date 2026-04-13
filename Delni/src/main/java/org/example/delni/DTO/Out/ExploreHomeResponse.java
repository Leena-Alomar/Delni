package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExploreHomeResponse {

    private Integer cityId;
    private String cityName;
    private String heroTitle;
    private String heroSubtitle;
    private String summary;
    private Integer totalPlaces;
    private Integer trendingCount;
    private Integer topRatedCount;
    private Integer savedCount;
    private List<String> quickNotes;
    private List<ExploreCategoryResponse> categories;
    private List<PlaceCardResponse> featuredPlaces;
    private List<PlaceCardResponse> trendingPlaces;
    private List<PlaceCardResponse> topRatedPlaces;
    private List<FavoritePlaceResponse> savedPlaces;
}
