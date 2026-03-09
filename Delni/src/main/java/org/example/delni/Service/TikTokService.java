package org.example.delni.Service;

import org.example.delni.DTO.TikTokResponse;
import org.example.delni.Model.Place;
import org.example.delni.Repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class TikTokService {

    @Value("${tiktok.api.key}")
    private String apiKey;

    @Autowired
    private PlaceRepository placeRepository;

    public double fetchTrendScore(String keyword) {

        String url =
                "https://tiktok-api23.p.rapidapi.com/api/search/general"
                        + "?keyword=" + keyword
                        + "&cursor=0"
                        + "&search_id=0";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", apiKey);
        headers.set("x-rapidapi-host", "tiktok-api23.p.rapidapi.com");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<TikTokResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        TikTokResponse.class
                );

        TikTokResponse body = response.getBody();

        double score = 0;

        if (body != null && body.getData() != null) {

            for (TikTokResponse.Data video : body.getData()) {

                long views = video.getStats().getPlayCount();
                long likes = video.getStats().getDiggCount();

                score += (views * 0.00001) + (likes * 0.001);
            }
        }

        return score;
    }

    // Step 4 method
    public void updatePlaceTrend(Place place) {

        double trendScore = fetchTrendScore(place.getName());

        place.setTiktokTrendScore(trendScore);

        if (trendScore > 20) {
            place.setIsTrending(true);
        } else {
            place.setIsTrending(false);
        }

        double smartScore =
                place.getGoogleRating() + trendScore;

        place.setSmartScore(smartScore);

        placeRepository.save(place);
    }

    public void updateAllTrends() {

        List<Place> places = placeRepository.findAll();

        for (Place place : places) {
            updatePlaceTrend(place);
        }
    }

    @Scheduled(fixedRate = 21600000)
    public void scheduledTrendUpdate() {

        System.out.println("Updating TikTok trends...");

        updateAllTrends();
    }
}