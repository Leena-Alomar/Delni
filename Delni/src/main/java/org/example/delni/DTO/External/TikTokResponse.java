package org.example.delni.DTO.External;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class TikTokResponse {

    private List<Entry> data;

    @Data
    public static class Entry {
        private Integer type;
        private Stats stats;
        private Item item;
    }

    @Data
    public static class Item {
        private String id;
        private String desc;
        private Long createTime;
        private Stats stats;
        private Author author;
        private Video video;
    }

    @Data
    public static class Stats {
        private long playCount;
        private long diggCount;
    }

    @Data
    public static class Author {
        private String nickname;
        private String uniqueId;
    }

    @Data
    public static class Video {
        private String cover;
        private String dynamicCover;
        @JsonProperty("playAddr")
        private String playAddr;
        @JsonProperty("playAddrH264")
        private String playAddrH264;
        @JsonProperty("playAddrBytevc1")
        private String playAddrBytevc1;
        @JsonProperty("downloadAddr")
        private String downloadAddr;
        @JsonProperty("playAddrUrlList")
        private List<String> playAddrUrlList;
    }
}
