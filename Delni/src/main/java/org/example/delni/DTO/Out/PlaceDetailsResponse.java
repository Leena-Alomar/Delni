package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDetailsResponse {

    private Integer id;
    private String name;
    private String description;
    private String cityName;
    private String cityRegion;
    private String category;
    private String vibeTag;
    private Double googleRating;
    private Double tiktokTrendScore;
    private Double smartScore;
    private Boolean trending;
    private String imageUrl;
    private String heroMediaUrl;
    private String googleMapsUrl;
    private String tiktokVideoUrl;
    private String trendReason;
    private String subtitle;
    private String scoreSummary;
    private String trendSummary;
    private String hoursSummary;
    private String recommendationSummary;
    private List<String> tags;
    private List<ActionLink> actionLinks;
    private List<MediaItem> media;
    private List<OperatingHourItem> operatingHours;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionLink {
        private String label;
        private String type;
        private String url;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaItem {
        private Integer id;
        private String mediaType;
        private String url;
        private String thumbnail;
        private String caption;
        private String sourcePlatform;
        private String externalMediaId;
        private String creatorName;
        private String creatorHandle;
        private Long viewCount;
        private Long likeCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperatingHourItem {
        private Integer dayOfWeek;
        private String openTime;
        private String closeTime;
        private Boolean splitShift;
        private String secondOpen;
        private String secondClose;
        private Boolean prayerBreak;
        private String notes;
    }
}
