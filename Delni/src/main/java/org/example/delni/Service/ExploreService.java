package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.DTO.Out.ExploreCategoryResponse;
import org.example.delni.DTO.Out.ExploreHomeResponse;
import org.example.delni.DTO.Out.FavoritePlaceResponse;
import org.example.delni.DTO.Out.PlaceCardResponse;
import org.example.delni.Model.City;
import org.example.delni.Model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExploreService {

    private final CityService cityService;
    private final PlaceService placeService;
    private final FavoriteService favoriteService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public ExploreHomeResponse getHome(Integer cityId, Integer userId) {
        City city = cityService.findCityById(cityId);
        User user = userId != null ? userService.findUserById(userId) : null;

        List<PlaceCardResponse> allPlaces = placeService.searchPlaceCards(cityId, null, null, null);
        List<PlaceCardResponse> trendingPlaces = allPlaces.stream()
                .filter(place -> Boolean.TRUE.equals(place.getTrending()))
                .limit(6)
                .toList();
        List<PlaceCardResponse> topRatedPlaces = allPlaces.stream()
                .sorted(Comparator.comparing(PlaceCardResponse::getGoogleRating, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .toList();
        List<PlaceCardResponse> featuredPlaces = selectFeaturedPlaces(allPlaces, user);
        List<FavoritePlaceResponse> savedPlaces = userId != null
                ? favoriteService.getFavoritePlaces(userId).stream().limit(5).toList()
                : List.of();

        return new ExploreHomeResponse(
                city.getId(),
                city.getName(),
                buildHeroTitle(city, user),
                buildHeroSubtitle(city, allPlaces, trendingPlaces),
                buildSummary(city, allPlaces, trendingPlaces, savedPlaces),
                allPlaces.size(),
                trendingPlaces.size(),
                topRatedPlaces.size(),
                savedPlaces.size(),
                buildQuickNotes(trendingPlaces, topRatedPlaces, savedPlaces),
                buildCategorySummary(allPlaces),
                featuredPlaces,
                trendingPlaces,
                topRatedPlaces,
                savedPlaces
        );
    }

    private List<PlaceCardResponse> selectFeaturedPlaces(List<PlaceCardResponse> allPlaces, User user) {
        if (allPlaces.isEmpty()) {
            return List.of();
        }

        List<String> interests = parseInterests(user);
        if (interests.isEmpty()) {
            return allPlaces.stream().limit(6).toList();
        }

        List<PlaceCardResponse> matching = allPlaces.stream()
                .filter(place -> matchesInterests(place, interests))
                .limit(6)
                .toList();
        return matching.isEmpty() ? allPlaces.stream().limit(6).toList() : matching;
    }

    private boolean matchesInterests(PlaceCardResponse place, List<String> interests) {
        String haystack = String.join(" ",
                safe(place.getCategory()),
                safe(place.getVibeTag()),
                safe(place.getDescription()),
                safe(place.getName())
        ).toLowerCase(Locale.ROOT);

        return interests.stream().anyMatch(haystack::contains);
    }

    private List<String> parseInterests(User user) {
        if (user == null || user.getInterestTags() == null || user.getInterestTags().isBlank()) {
            return List.of();
        }

        return java.util.Arrays.stream(user.getInterestTags().split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .toList();
    }

    private List<ExploreCategoryResponse> buildCategorySummary(List<PlaceCardResponse> places) {
        Map<String, Long> categoryCounts = places.stream()
                .map(place -> safe(place.getCategory()))
                .filter(category -> !category.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return categoryCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(entry -> new ExploreCategoryResponse(
                        entry.getKey(),
                        capitalizeWords(entry.getKey().replace('_', ' ')),
                        entry.getValue().intValue()))
                .toList();
    }

    private String buildHeroTitle(City city, User user) {
        if (user != null && user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return "Explore " + city.getName() + " for " + user.getFirstName();
        }
        return "Explore " + city.getName();
    }

    private String buildHeroSubtitle(City city, List<PlaceCardResponse> allPlaces, List<PlaceCardResponse> trendingPlaces) {
        return allPlaces.size() + " place(s) ready for discovery in " + city.getName()
                + (trendingPlaces.isEmpty() ? "." : ", including " + trendingPlaces.size() + " trending pick(s).");
    }

    private String buildSummary(City city, List<PlaceCardResponse> allPlaces, List<PlaceCardResponse> trendingPlaces,
                                List<FavoritePlaceResponse> savedPlaces) {
        return city.getName() + " has " + allPlaces.size() + " browse-ready place card(s), "
                + trendingPlaces.size() + " trending result(s), and "
                + savedPlaces.size() + " saved place(s) in this view.";
    }

    private List<String> buildQuickNotes(List<PlaceCardResponse> trendingPlaces,
                                         List<PlaceCardResponse> topRatedPlaces,
                                         List<FavoritePlaceResponse> savedPlaces) {
        List<String> notes = new ArrayList<>();
        if (!trendingPlaces.isEmpty()) {
            notes.add("Trending section is ready");
        }
        if (!topRatedPlaces.isEmpty()) {
            notes.add("Top-rated section is ready");
        }
        if (!savedPlaces.isEmpty()) {
            notes.add("Saved places section is ready");
        }
        notes.add("All cards include direct map actions");
        return notes;
    }

    private String capitalizeWords(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return java.util.Arrays.stream(value.trim().split("\\s+"))
                .filter(part -> !part.isBlank())
                .map(this::capitalize)
                .collect(Collectors.joining(" "));
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
