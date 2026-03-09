package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.DTO.GoogleResponse;
import org.example.delni.Model.Place;
import org.example.delni.Repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class GoogleMapsService {

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    @Autowired
    private PlaceRepository placeRepository;

    public List<Place> syncNearbyPlaces(double lat, double lng, String type) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=3000"
                + "&type=" + type
                + "&key=" + googleMapsApiKey
                + "&language=ar";

        RestTemplate restTemplate = new RestTemplate();

        // 1. Fix Arabic encoding immediately
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        // 2. Get the response from Google
        GoogleResponse response = restTemplate.getForObject(url, GoogleResponse.class);

        List<Place> savedPlaces = new ArrayList<>();

        if (response != null && response.getResults() != null) {
            for (GoogleResponse.GooglePlaceDTO dto : response.getResults()) {

                // Check if place exists, if not, create and save
                Place place = placeRepository.findByName(dto.getName())
                        .orElseGet(() -> {
                            Place newPlace = new Place();
                            newPlace.setName(dto.getName());
                            newPlace.setGoogleRating(dto.getRating());
                            newPlace.setLatitude(dto.getGeometry().getLocation().getLat());
                            newPlace.setLongitude(dto.getGeometry().getLocation().getLng());
                            newPlace.setCategory(type);

                            // Initialize Smart Score (Google Rating is the baseline)
                            newPlace.setSmartScore(dto.getRating() != null ? dto.getRating() : 0.0);

                            // Initialize TikTok stats as null/zero for now
                            newPlace.setIsTrending(false);
                            newPlace.setTiktokTrendScore(0.0);

                            return placeRepository.save(newPlace);
                        });

                // 3. Trigger TikTok Sync via n8n if trend score hasn't been calculated yet
                if (place.getTiktokTrendScore() == null || place.getTiktokTrendScore() == 0.0) {
                    triggerTikTokSync(place);
                }

                savedPlaces.add(place);
            }
        }

        return savedPlaces;
    }

    //Sends a request to n8n to start the TikTok scraping and AI analysis

    public void triggerTikTokSync(Place place) {
        RestTemplate restTemplate = new RestTemplate();

        // Create the payload for n8n
        Map<String, Object> payload = new HashMap<>();
        payload.put("placeId", place.getId());
        payload.put("placeName", place.getName());
        payload.put("city", "Yanbu"); // Static for now, can be dynamic later

        try {
            // We use postForObject to send the data to your n8n Webhook URL
            restTemplate.postForObject(n8nWebhookUrl, payload, String.class);
            System.out.println("Triggered n8n TikTok sync for: " + place.getName());
        } catch (Exception e) {
            // Log the error but don't stop the whole process if n8n is down
            System.err.println("n8n Trigger failed for " + place.getName() + ": " + e.getMessage());
        }
    }
}