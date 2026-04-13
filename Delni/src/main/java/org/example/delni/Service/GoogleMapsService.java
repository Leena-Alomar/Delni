package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiException;
import org.example.delni.DTO.External.GoogleResponse;
import org.example.delni.DTO.Out.GoogleSyncResponse;
import org.example.delni.Model.City;
import org.example.delni.Model.Place;
import org.example.delni.Repository.CityRepository;
import org.example.delni.Repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class GoogleMapsService {

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private final CityRepository cityRepository;
    private final PlaceRepository placeRepository;
    private final TikTokService tikTokService;
    private final PlaceService placeService;
    private final RestTemplate restTemplate;

    public GoogleSyncResponse syncNearbyPlaces(double lat, double lng, String type, Integer cityId) {
        if (googleMapsApiKey == null || googleMapsApiKey.isBlank()) {
            throw new ApiException("Google Maps API key is not configured");
        }

        City city = resolveCity(cityId, lat, lng);

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                + "?location=" + lat + "," + lng
                + "&radius=3000"
                + "&type=" + type
                + "&key=" + googleMapsApiKey
                + "&language=ar";

        GoogleResponse response = restTemplate.getForObject(url, GoogleResponse.class);

        List<Place> savedPlaces = new ArrayList<>();

        if (response != null && response.getResults() != null) {
            for (GoogleResponse.GooglePlaceDTO dto : response.getResults()) {
                if (dto.getGeometry() == null || dto.getGeometry().getLocation() == null) {
                    continue;
                }

                Place place = findExistingPlace(dto, city)
                        .orElseGet(Place::new);

                place.setName(dto.getName());
                place.setGoogleRating(dto.getRating() != null ? dto.getRating() : 0.0);
                place.setLatitude(dto.getGeometry().getLocation().getLat());
                place.setLongitude(dto.getGeometry().getLocation().getLng());
                place.setCategory(type);
                place.setCity(city);
                place.setGooglePlaceId(dto.getPlace_id());
                if ((place.getDescription() == null || place.getDescription().isBlank()) && dto.getVicinity() != null) {
                    place.setDescription(dto.getVicinity());
                }
                place.setGoogleMapsUrl("https://www.google.com/maps/search/?api=1&query="
                        + dto.getGeometry().getLocation().getLat() + "," + dto.getGeometry().getLocation().getLng());

                if (place.getTiktokTrendScore() == null) {
                    place.setTiktokTrendScore(0.0);
                }

                if (place.getIsTrending() == null) {
                    place.setIsTrending(false);
                }

                place.setSmartScore(place.getGoogleRating() + place.getTiktokTrendScore());
                place = placeRepository.save(place);


                if (place.getTiktokTrendScore() == null || place.getTiktokTrendScore() == 0.0) {
                    if (tikTokService.hasApiKey()) {
                        tikTokService.updatePlaceTrend(place);
                    } else if (place.getTrendReason() == null || place.getTrendReason().isBlank()) {
                        place.setTrendReason("Imported from Google Maps. TikTok trend refresh is available once RAPIDAPI_KEY is configured.");
                        placeRepository.save(place);
                    }
                }

                savedPlaces.add(place);
            }
        }

        int trendingCount = (int) savedPlaces.stream()
                .filter(place -> Boolean.TRUE.equals(place.getIsTrending()))
                .count();

        return new GoogleSyncResponse(
                city.getId(),
                city.getName(),
                type,
                type,
                savedPlaces.size(),
                trendingCount,
                savedPlaces.isEmpty()
                        ? "No places were imported from Google Maps for this query."
                        : "Imported " + savedPlaces.size() + " place(s) from Google Maps.",
                savedPlaces.isEmpty()
                        ? "No places found"
                        : city.getName() + " " + type + " spots synced",
                savedPlaces.isEmpty()
                        ? "Try a different Google place type or search area."
                        : savedPlaces.size() + " place(s) imported and sorted for the explore screen. "
                        + trendingCount + " already have TikTok trending signals.",
                tikTokService.hasApiKey()
                        ? "Open /api/places?cityId=" + city.getId() + " to browse cards, or /api/trends/update to refresh all trend scores."
                        : "Google sync worked. Add RAPIDAPI_KEY when you want TikTok trend enrichment too.",
                placeService.toPlaceCards(savedPlaces)
        );
    }

    private Optional<Place> findExistingPlace(GoogleResponse.GooglePlaceDTO dto, City city) {
        if (dto.getPlace_id() != null && !dto.getPlace_id().isBlank()) {
            Optional<Place> placeByGoogleId = placeRepository.findByGooglePlaceIdAndCityId(dto.getPlace_id(), city.getId());
            if (placeByGoogleId.isPresent()) {
                return placeByGoogleId;
            }
        }

        List<Place> sameNamePlaces = placeRepository.findAllByCityIdAndNameIgnoreCase(city.getId(), dto.getName());
        if (sameNamePlaces.isEmpty()) {
            return Optional.empty();
        }

        Place closestPlace = sameNamePlaces.stream()
                .min((first, second) -> Double.compare(
                        haversine(first.getLatitude(), first.getLongitude(),
                                dto.getGeometry().getLocation().getLat(), dto.getGeometry().getLocation().getLng()),
                        haversine(second.getLatitude(), second.getLongitude(),
                                dto.getGeometry().getLocation().getLat(), dto.getGeometry().getLocation().getLng())))
                .orElse(null);

        if (closestPlace == null) {
            return Optional.empty();
        }

        double distanceKm = haversine(
                closestPlace.getLatitude(),
                closestPlace.getLongitude(),
                dto.getGeometry().getLocation().getLat(),
                dto.getGeometry().getLocation().getLng()
        );

        return distanceKm <= 0.1 ? Optional.of(closestPlace) : Optional.empty();
    }

    private City resolveCity(Integer cityId, double lat, double lng) {
        if (cityId != null) {
            return cityRepository.findById(cityId)
                    .orElseThrow(() -> new ApiException("City not found"));
        }

        return cityRepository.findAll().stream()
                .min((first, second) -> Double.compare(
                        haversine(first.getLatitude(), first.getLongitude(), lat, lng),
                        haversine(second.getLatitude(), second.getLongitude(), lat, lng)))
                .orElseThrow(() -> new ApiException("No city available to link synced places"));
    }

    private double haversine(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return Double.MAX_VALUE;
        }

        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double originLat = Math.toRadians(lat1);
        double destinationLat = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(originLat) * Math.cos(destinationLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
