package org.example.delni.DTO;


import java.util.List;

public class TikTokResponse {

    private List<Data> data;

    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }

    public static class Data {

        private Stats stats;

        public Stats getStats() {
            return stats;
        }

        public void setStats(Stats stats) {
            this.stats = stats;
        }
    }

    public static class Stats {

        private long playCount;
        private long diggCount;

        public long getPlayCount() {
            return playCount;
        }

        public long getDiggCount() {
            return diggCount;
        }
    }
}