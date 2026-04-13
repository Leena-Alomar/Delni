package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiException;
import org.example.delni.DTO.Out.FavoritePlaceResponse;
import org.example.delni.DTO.Out.FavoriteResponse;
import org.example.delni.Model.Favorite;
import org.example.delni.Model.Place;
import org.example.delni.Model.User;
import org.example.delni.Repository.FavoriteRepository;
import org.example.delni.Repository.PlaceRepository;
import org.example.delni.Repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    @Transactional(readOnly = true)
    public List<Favorite> getAllFavorites() {
        return favoriteRepository.findAll();
    }

    @Transactional
    public Favorite addFavorite(Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public Favorite updateFavorite(Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void deleteFavorite(Integer id) {
        favoriteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Favorite findFavoriteById(Integer id) {
        return favoriteRepository.findFavoriteById(id);
    }

    @Transactional(readOnly = true)
    public Favorite findFavoriteByUserIdAndPlaceId(Integer userId, Integer placeId) {
        return favoriteRepository.findFavoriteByUserIdAndPlaceId(userId, placeId);
    }

    @Transactional(readOnly = true)
    public List<Favorite> getFavoritesByUserId(Integer userId) {
        return favoriteRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavoriteResponsesByUserId(Integer userId) {
        return favoriteRepository.findAllByUserId(userId).stream()
                .map(this::toFavoriteResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FavoritePlaceResponse> getFavoritePlaces(Integer userId) {
        return favoriteRepository.findAllByUserId(userId).stream()
                .map(favorite -> new FavoritePlaceResponse(
                        favorite.getId(),
                        favorite.getSavedAt(),
                        favorite.getPlace().getId(),
                        favorite.getPlace().getName(),
                        favorite.getPlace().getCategory(),
                        favorite.getPlace().getVibeTag(),
                        favorite.getPlace().getCity() != null ? favorite.getPlace().getCity().getName() : null,
                        favorite.getPlace().getGoogleRating(),
                        favorite.getPlace().getIsTrending(),
                        favorite.getPlace().getImageUrl(),
                        favorite.getPlace().getDescription(),
                        favorite.getPlace().getGoogleMapsUrl(),
                        buildPlaceSubtitle(favorite.getPlace()),
                        buildScoreLabel(favorite.getPlace()),
                        buildTrendLabel(favorite.getPlace()),
                        buildBadges(favorite.getPlace())))
                .toList();
    }

    @Transactional
    public Favorite saveFavorite(Integer userId, Integer placeId) {
        Favorite existingFavorite = favoriteRepository.findFavoriteByUserIdAndPlaceId(userId, placeId);
        if (existingFavorite != null) {
            return existingFavorite;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found"));
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException("Place not found"));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setPlace(place);
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public FavoriteResponse saveFavoriteResponse(Integer userId, Integer placeId) {
        return toFavoriteResponse(saveFavorite(userId, placeId));
    }

    @Transactional
    public void removeFavorite(Integer userId, Integer placeId) {
        Favorite favorite = favoriteRepository.findFavoriteByUserIdAndPlaceId(userId, placeId);
        if (favorite == null) {
            throw new ApiException("Favorite not found");
        }

        favoriteRepository.delete(favorite);
    }

    private FavoriteResponse toFavoriteResponse(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getSavedAt(),
                favorite.getUser() != null ? favorite.getUser().getId() : null,
                favorite.getPlace() != null ? favorite.getPlace().getId() : null,
                favorite.getPlace() != null ? favorite.getPlace().getName() : null,
                favorite.getPlace() != null ? favorite.getPlace().getCategory() : null,
                favorite.getPlace() != null && favorite.getPlace().getCity() != null
                        ? favorite.getPlace().getCity().getName() : null,
                favorite.getPlace() != null ? favorite.getPlace().getGoogleRating() : null,
                favorite.getPlace() != null ? favorite.getPlace().getIsTrending() : null,
                favorite.getPlace() != null ? favorite.getPlace().getImageUrl() : null
        );
    }

    private String buildPlaceSubtitle(Place place) {
        List<String> parts = new ArrayList<>();
        if (place.getCity() != null && place.getCity().getName() != null) {
            parts.add(place.getCity().getName());
        }
        if (place.getCategory() != null && !place.getCategory().isBlank()) {
            parts.add(capitalize(place.getCategory().replace('_', ' ')));
        }
        if (place.getVibeTag() != null && !place.getVibeTag().isBlank()) {
            parts.add(capitalize(place.getVibeTag()));
        }
        return parts.isEmpty() ? "Saved place" : String.join(" • ", parts);
    }

    private String buildScoreLabel(Place place) {
        double googleRating = place.getGoogleRating() != null ? place.getGoogleRating() : 0.0;
        double trendScore = place.getTiktokTrendScore() != null ? place.getTiktokTrendScore() : 0.0;
        return "Google " + formatOneDecimal(googleRating) + " • TikTok " + formatOneDecimal(trendScore);
    }

    private String buildTrendLabel(Place place) {
        if (Boolean.TRUE.equals(place.getIsTrending())) {
            return "Trending on TikTok";
        }
        if (place.getTiktokTrendScore() != null && place.getTiktokTrendScore() > 0) {
            return "Has TikTok signal";
        }
        return "Saved recommendation";
    }

    private List<String> buildBadges(Place place) {
        Set<String> badges = new LinkedHashSet<>();
        addBadge(badges, place.getCategory());
        addBadge(badges, place.getVibeTag());
        if (Boolean.TRUE.equals(place.getIsTrending())) {
            badges.add("Trending");
        }
        if (place.getGoogleRating() != null && place.getGoogleRating() >= 4.5) {
            badges.add("Top rated");
        }
        return new ArrayList<>(badges);
    }

    private void addBadge(Set<String> badges, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        badges.add(capitalize(value.replace('_', ' ')));
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private String formatOneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
