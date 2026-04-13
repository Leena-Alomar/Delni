package org.example.delni.DTO.External;

import lombok.Data;
import java.util.List;

@Data
public class GoogleResponse {
    private List<GooglePlaceDTO> results;
    private String status;

    @Data
    public static class GooglePlaceDTO {
        private String name;
        private Double rating;
        private Geometry geometry;
        private String vicinity;
        private String place_id;

        @Data
        public static class Geometry {
            private Location location;
        }

        @Data
        public static class Location {
            private Double lat;
            private Double lng;
        }
    }
}
