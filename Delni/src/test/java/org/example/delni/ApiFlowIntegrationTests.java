package org.example.delni;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.example.delni.Model.City;
import org.example.delni.Model.Favorite;
import org.example.delni.Model.Place;
import org.example.delni.Model.Trip;
import org.example.delni.Model.TripPlace;
import org.example.delni.Model.User;
import org.example.delni.Repository.CityRepository;
import org.example.delni.Repository.FavoriteRepository;
import org.example.delni.Repository.PlaceRepository;
import org.example.delni.Repository.TripPlaceRepository;
import org.example.delni.Repository.TripRepository;
import org.example.delni.Repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripPlaceRepository tripPlaceRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @BeforeEach
    void cleanDatabase() {
        favoriteRepository.deleteAll();
        tripPlaceRepository.deleteAll();
        tripRepository.deleteAll();
        placeRepository.deleteAll();
        userRepository.deleteAll();
        cityRepository.deleteAll();
    }

    @Test
    void coreFlowWorksAcrossMainEndpoints() throws Exception {
        int cityId = createCity();
        int userId = createUser();
        int firstPlaceId = createPlace(cityId, "Bateel", "cafe", "modern", 24.7262, 46.6385, 4.8);
        createPlace(cityId, "Najd Village", "restaurant", "heritage", 24.7111, 46.6702, 4.6);
        createPlace(cityId, "Boulevard World", "entertainment", "modern", 24.7686, 46.6042, 4.7);
        createPlace(cityId, "National Museum", "museum", "history", 24.6463, 46.7102, 4.5);

        mockMvc.perform(get("/api/explore/home")
                        .param("cityId", String.valueOf(cityId))
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cityId").value(cityId))
                .andExpect(jsonPath("$.featuredPlaces").isArray())
                .andExpect(jsonPath("$.topRatedPlaces").isArray());

        mockMvc.perform(get("/api/places/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate;

        mockMvc.perform(post("/api/trips/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Integration Trip",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "groupSize": 2,
                                  "tripType": "Family",
                                  "budgetTier": "Medium",
                                  "totalCostEstimate": 500,
                                  "includeTikTokTrending": false,
                                  "weatherAware": false,
                                  "includePrayerSchedule": false,
                                  "includeTopRatings": true,
                                  "userId": %d,
                                  "cityId": %d
                                }
                                """.formatted(startDate, endDate, userId, cityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Integration Trip"));

        Trip trip = tripRepository.findAll().stream()
                .filter(candidate -> "Integration Trip".equals(candidate.getTitle()))
                .max(Comparator.comparing(Trip::getId))
                .orElseThrow();
        int tripId = trip.getId();

        mockMvc.perform(get("/api/trips/{id}/itinerary", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days[0].blocks[0].places[0].tripPlaceId").exists());

        TripPlace firstTripPlace = tripPlaceRepository.findAllByTripIdOrderByDayNumberAscOrderInDayAsc(tripId).get(0);
        int tripPlaceId = firstTripPlace.getId();

        mockMvc.perform(get("/api/places/{id}/details", firstPlaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstPlaceId))
                .andExpect(jsonPath("$.tags").isArray());

        mockMvc.perform(post("/api/favorites")
                        .param("userId", String.valueOf(userId))
                        .param("placeId", String.valueOf(firstPlaceId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/favorites/user/{userId}/places", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].placeId").value(firstPlaceId));

        Favorite favorite = favoriteRepository.findFavoriteByUserIdAndPlaceId(userId, firstPlaceId);
        assertNotNull(favorite);

        mockMvc.perform(put("/api/trips/{tripId}/places/{tripPlaceId}/replace", tripId, tripPlaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tripId").value(tripId));

        mockMvc.perform(delete("/api/favorites")
                        .param("userId", String.valueOf(userId))
                        .param("placeId", String.valueOf(firstPlaceId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/favorites/user/{userId}/places", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        assertFalse(favoriteRepository.findAllByUserId(userId).stream()
                .anyMatch(saved -> saved.getPlace().getId().equals(firstPlaceId)));
    }

    @Test
    void updatesPreserveExistingStateForUserTripAndPlaceDetails() throws Exception {
        int cityId = createCity();
        int userId = createUser();

        int detailedPlaceId = createDetailedPlace(cityId);
        Place originalPlace = placeRepository.findById(detailedPlaceId).orElseThrow();
        assertNotNull(originalPlace.getId());

        mockMvc.perform(put("/api/places/{id}", detailedPlaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Detail Place",
                                  "description": "Updated description",
                                  "category": "cafe",
                                  "vibeTag": "modern",
                                  "latitude": 24.7201,
                                  "longitude": 46.6311,
                                  "googleRating": 4.7,
                                  "cityId": %d
                                }
                                """.formatted(cityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Detail Place"));

        mockMvc.perform(get("/api/places/{id}/details", detailedPlaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media.length()").value(1))
                .andExpect(jsonPath("$.operatingHours.length()").value(1));

        User originalUser = userRepository.findById(userId).orElseThrow();
        assertNotNull(originalUser.getCreatedAt());

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "سارة",
                                  "lastName": "المحدثة",
                                  "username": "integration_user_updated",
                                  "email": "integration.updated@example.com",
                                  "password": "Strong@123",
                                  "phoneNumber": "966512345679",
                                  "interestTags": "coffee,history,culture",
                                  "aiPreferenceSummary": "Updated preferences"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integration_user_updated"));

        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertEquals(originalUser.getCreatedAt(), updatedUser.getCreatedAt());

        createPlace(cityId, "Trip Cafe", "cafe", "modern", 24.7262, 46.6385, 4.8);
        createPlace(cityId, "Trip Dinner", "restaurant", "heritage", 24.7111, 46.6702, 4.6);

        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = startDate;

        mockMvc.perform(post("/api/trips/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Preserve Trip",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "groupSize": 2,
                                  "tripType": "Friends",
                                  "budgetTier": "Medium",
                                  "includeTikTokTrending": false,
                                  "weatherAware": false,
                                  "includePrayerSchedule": false,
                                  "includeTopRatings": true,
                                  "userId": %d,
                                  "cityId": %d
                                }
                                """.formatted(startDate, endDate, userId, cityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Preserve Trip"));

        Trip originalTrip = tripRepository.findAll().stream()
                .filter(candidate -> "Preserve Trip".equals(candidate.getTitle()))
                .max(Comparator.comparing(Trip::getId))
                .orElseThrow();
        assertNotNull(originalTrip.getCreatedAt());
        int tripId = originalTrip.getId();
        int originalTripPlaceCount = tripPlaceRepository.findAllByTripIdOrderByDayNumberAscOrderInDayAsc(tripId).size();

        mockMvc.perform(put("/api/trips/{id}", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Preserve Trip Updated",
                                  "startDate": "%s",
                                  "endDate": "%s",
                                  "daysCount": 1,
                                  "groupSize": 3,
                                  "tripType": "Friends",
                                  "budgetTier": "Medium",
                                  "totalCostEstimate": 650,
                                  "includeTikTokTrending": false,
                                  "weatherAware": false,
                                  "includePrayerSchedule": false,
                                  "includeTopRatings": true,
                                  "userPrompt": "Keep it smooth",
                                  "aiItineraryLogic": "Existing itinerary should stay attached",
                                  "userId": %d,
                                  "cityId": %d
                                }
                                """.formatted(startDate, endDate, userId, cityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Preserve Trip Updated"));

        Trip updatedTrip = tripRepository.findById(tripId).orElseThrow();
        assertEquals(originalTrip.getCreatedAt(), updatedTrip.getCreatedAt());
        assertEquals(originalTripPlaceCount,
                tripPlaceRepository.findAllByTripIdOrderByDayNumberAscOrderInDayAsc(tripId).size());
    }

    private int createCity() throws Exception {
        mockMvc.perform(post("/api/cities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Riyadh",
                                  "region": "Riyadh",
                                  "latitude": 24.7136,
                                  "longitude": 46.6753,
                                  "description": "Integration test city"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Riyadh"));

        City city = cityRepository.findCityByName("Riyadh");
        assertNotNull(city);
        return city.getId();
    }

    private int createUser() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "سارة",
                                  "lastName": "أحمد",
                                  "username": "integration_user",
                                  "email": "integration@example.com",
                                  "password": "Strong@123",
                                  "phoneNumber": "966512345678",
                                  "interestTags": "coffee,history",
                                  "aiPreferenceSummary": "Varied local experiences"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integration_user"));

        User user = userRepository.findUserByUsername("integration_user");
        assertNotNull(user);
        assertEquals("integration_user", user.getUsername());
        return user.getId();
    }

    private int createPlace(int cityId, String name, String category, String vibeTag,
                            double latitude, double longitude, double rating) throws Exception {
        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "%s description",
                                  "category": "%s",
                                  "vibeTag": "%s",
                                  "latitude": %s,
                                  "longitude": %s,
                                  "googleRating": %s,
                                  "cityId": %d
                                }
                                """.formatted(name, name, category, vibeTag, latitude, longitude, rating, cityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));

        Place place = placeRepository.findByName(name).orElseThrow();
        assertTrue(place.getSmartScore() >= rating);
        return place.getId();
    }

    private int createDetailedPlace(int cityId) throws Exception {
        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Detailed Place",
                                  "description": "Place with media and hours",
                                  "category": "cafe",
                                  "vibeTag": "modern",
                                  "latitude": 24.7201,
                                  "longitude": 46.6311,
                                  "googleRating": 4.5,
                                  "cityId": %d,
                                  "operatingHours": [
                                    {
                                      "dayOfWeek": 1,
                                      "openTime": "09:00:00",
                                      "closeTime": "23:00:00",
                                      "splitShift": false,
                                      "prayerBreak": true,
                                      "notes": "Open all day"
                                    }
                                  ],
                                  "mediaList": [
                                    {
                                      "mediaType": "image",
                                      "url": "https://example.com/image.jpg",
                                      "thumbnail": "https://example.com/thumb.jpg",
                                      "caption": "Hero image",
                                      "sourcePlatform": "manual"
                                    }
                                  ]
                                }
                                """.formatted(cityId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Detailed Place"));

        return placeRepository.findByName("Detailed Place").orElseThrow().getId();
    }
}
