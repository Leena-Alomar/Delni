package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendRefreshResponse {

    private String status;
    private String summaryTitle;
    private String summarySubtitle;
    private String searchStrategy;
    private String aiPrompt;
    private Integer totalPlaces;
    private Integer updatedPlaces;
    private Integer trendingPlaces;
    private Integer noResultPlaces;
    private Integer skippedPlaces;
    private String message;
    private List<TrendPlaceResult> places;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPlaceResult {
        private Integer placeId;
        private String name;
        private String cityName;
        private Double googleRating;
        private Double tiktokTrendScore;
        private Double smartScore;
        private Boolean trending;
        private String trendReason;
        private String status;
        private String matchedKeyword;
        private List<String> attemptedKeywords;
        private String searchPrompt;
        private String scoreSummary;
        private String recommendation;
        private Integer matchedItems;
        private Integer recentVideoCount;
        private Integer localVideoCount;
        private Integer recentLocalVideoCount;
        private Integer trendWindowDays;
        private List<TikTokMediaPreviewResponse> mediaPreviews;
    }
}
