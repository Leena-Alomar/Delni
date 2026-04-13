package org.example.delni.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "tiktok.schedule.enabled", havingValue = "true", matchIfMissing = true)
public class TikTokTrendScheduler {

    private static final Logger log = LoggerFactory.getLogger(TikTokTrendScheduler.class);

    private final TikTokService tikTokService;

    public TikTokTrendScheduler(TikTokService tikTokService) {
        this.tikTokService = tikTokService;
    }

    @Scheduled(fixedRateString = "${tiktok.schedule.fixed-rate-ms:21600000}")
    public void scheduledTrendUpdate() {
        log.info("Refreshing TikTok trend scores for all places");
        tikTokService.updateAllTrends();
    }
}
