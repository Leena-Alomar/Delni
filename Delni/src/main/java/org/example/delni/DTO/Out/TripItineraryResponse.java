package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripItineraryResponse {

    private Integer tripId;
    private String title;
    private String cityName;
    private String tripType;
    private Integer groupSize;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer daysCount;
    private Double totalCostEstimate;
    private Boolean includeTikTokTrending;
    private Boolean weatherAware;
    private Boolean includePrayerSchedule;
    private Boolean includeTopRatings;
    private String headerTitle;
    private String headerSubtitle;
    private String planningSummary;
    private List<String> quickNotes;
    private Summary summary;
    private List<TripDay> days;
    private List<MapPoint> mapPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private String city;
        private Integer durationDays;
        private Double estimatedCost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripDay {
        private Integer dayNumber;
        private String label;
        private LocalDate date;
        private WeatherSummary weather;
        private PrayerSchedule prayerSchedule;
        private List<TimeBlock> blocks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeBlock {
        private String timeSlot;
        private String displayTime;
        private String weatherNote;
        private String prayerNote;
        private Integer placesCount;
        private List<TripStop> places;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripStop {
        private Integer tripPlaceId;
        private Integer placeId;
        private Integer orderInDay;
        private String plannedTime;
        private String name;
        private String category;
        private String imageUrl;
        private String status;
        private Double googleRating;
        private Boolean trending;
        private Integer activityDuration;
        private Integer travelTimeMins;
        private String aiNote;
        private Double latitude;
        private Double longitude;
        private Boolean replaceable;
        private String subtitle;
        private String googleMapsUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MapPoint {
        private Integer placeId;
        private String name;
        private Double latitude;
        private Double longitude;
        private Integer dayNumber;
        private Integer orderInDay;
        private String timeSlot;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherSummary {
        private String summary;
        private Double minTemperature;
        private Double maxTemperature;
        private Integer precipitationProbability;
        private Boolean outdoorFriendly;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrayerSchedule {
        private String method;
        private String fajr;
        private String sunrise;
        private String dhuhr;
        private String asr;
        private String maghrib;
        private String isha;
    }
}
