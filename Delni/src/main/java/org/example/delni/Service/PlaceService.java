package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiException;
import org.example.delni.DTO.In.OperatingHoursRequest;
import org.example.delni.DTO.In.PlaceRequest;
import org.example.delni.DTO.Out.PlaceCardResponse;
import org.example.delni.DTO.Out.PlaceDetailsResponse;
import org.example.delni.Model.City;
import org.example.delni.Model.OperatingHours;
import org.example.delni.Model.Place;
import org.example.delni.Model.PlaceMedia;
import org.example.delni.Repository.CityRepository;
import org.example.delni.Repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final CityRepository cityRepository;
    private final PlaceMediaService placeMediaService;
    private final OperatingHoursService operatingHoursService;

    @Transactional(readOnly = true)
    public List<Place> getAllPlaces() {
        return sortPlaces(placeRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<PlaceCardResponse> getAllPlaceCards() {
        return toPlaceCards(getAllPlaces());
    }

    @Transactional(readOnly = true)
    public Place getPlaceById(Integer id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new ApiException("Place not found"));
    }

    @Transactional(readOnly = true)
    public PlaceCardResponse getPlaceCardById(Integer id) {
        return toPlaceCard(getPlaceById(id));
    }

    @Transactional(readOnly = true)
    public PlaceDetailsResponse getPlaceDetails(Integer id) {
        Place place = getPlaceById(id);
        List<PlaceMedia> mediaItems = placeMediaService.buildDetailsMedia(place);
        List<OperatingHours> operatingHours = operatingHoursService.getOrderedByPlaceId(id);

        List<String> tags = new ArrayList<>();
        addTag(tags, place.getCategory());
        addTag(tags, place.getVibeTag());
        if (place.getCity() != null) {
            addTag(tags, place.getCity().getName());
        }
        if (Boolean.TRUE.equals(place.getIsTrending())) {
            tags.add("trending");
        }

        List<PlaceDetailsResponse.ActionLink> actionLinks = new ArrayList<>();
        actionLinks.add(new PlaceDetailsResponse.ActionLink("Open in Google Maps", "google_maps", buildMapsUrl(place)));
        actionLinks.add(new PlaceDetailsResponse.ActionLink("Search event tickets", "ticket_search", buildSearchUrl(place.getName() + " tickets")));
        actionLinks.add(new PlaceDetailsResponse.ActionLink("Plan ride", "ride", buildMapsUrl(place)));
        if (place.getTiktokVideoUrl() != null && !place.getTiktokVideoUrl().isBlank()) {
            actionLinks.add(new PlaceDetailsResponse.ActionLink("Open TikTok", "tiktok", place.getTiktokVideoUrl()));
        }

        return new PlaceDetailsResponse(
                place.getId(),
                place.getName(),
                place.getDescription(),
                place.getCity() != null ? place.getCity().getName() : null,
                place.getCity() != null ? place.getCity().getRegion() : null,
                place.getCategory(),
                place.getVibeTag(),
                place.getGoogleRating(),
                place.getTiktokTrendScore(),
                place.getSmartScore(),
                place.getIsTrending(),
                place.getImageUrl(),
                buildHeroMediaUrl(place, mediaItems),
                buildMapsUrl(place),
                place.getTiktokVideoUrl(),
                place.getTrendReason(),
                buildPlaceSubtitle(place),
                buildScoreLabel(place),
                buildTrendLabel(place),
                buildHoursSummary(operatingHours),
                buildRecommendationSummary(place),
                tags,
                actionLinks,
                mediaItems.stream()
                        .map(media -> new PlaceDetailsResponse.MediaItem(
                                media.getId(),
                                media.getMediaType(),
                                media.getUrl(),
                                media.getThumbnail(),
                                media.getCaption(),
                                media.getSourcePlatform(),
                                media.getExternalMediaId(),
                                media.getCreatorName(),
                                media.getCreatorHandle(),
                                media.getViewCount(),
                                media.getLikeCount()))
                        .collect(Collectors.toList()),
                operatingHours.stream()
                        .map(hours -> new PlaceDetailsResponse.OperatingHourItem(
                                hours.getDayOfWeek(),
                                hours.getOpenTime() != null ? hours.getOpenTime().toString() : null,
                                hours.getCloseTime() != null ? hours.getCloseTime().toString() : null,
                                hours.getIsSplitShift(),
                                hours.getSecondOpen() != null ? hours.getSecondOpen().toString() : null,
                                hours.getSecondClose() != null ? hours.getSecondClose().toString() : null,
                                hours.getPrayerBreak(),
                                hours.getNotes()))
                        .collect(Collectors.toList())
        );
    }

    @Transactional
    public Place addPlace(Place place) {
        normalizePlaceForSave(place);
        return placeRepository.save(place);
    }

    @Transactional
    public PlaceCardResponse addPlaceResponse(Place place) {
        return toPlaceCard(addPlace(place));
    }

    @Transactional
    public PlaceCardResponse addPlaceResponse(PlaceRequest request) {
        return addPlaceResponse(toPlace(request));
    }

    @Transactional
    public Place updatePlace(Integer id, Place place) {
        Place existingPlace = getPlaceById(id);
        mergeIntoExistingPlace(existingPlace, place);
        normalizePlaceForSave(existingPlace);
        return placeRepository.save(existingPlace);
    }

    @Transactional
    public Place updatePlace(Integer id, PlaceRequest request) {
        Place existingPlace = getPlaceById(id);
        applyRequestToPlace(existingPlace, request);
        normalizePlaceForSave(existingPlace);
        return placeRepository.save(existingPlace);
    }

    @Transactional
    public PlaceCardResponse updatePlaceResponse(Integer id, Place place) {
        return toPlaceCard(updatePlace(id, place));
    }

    @Transactional
    public PlaceCardResponse updatePlaceResponse(Integer id, PlaceRequest request) {
        return toPlaceCard(updatePlace(id, request));
    }

    @Transactional
    public void deletePlace(Integer id) {
        getPlaceById(id);
        placeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Place> getTopPlaces() {
        return sortPlaces(placeRepository.findAllByOrderBySmartScoreDesc());
    }

    @Transactional(readOnly = true)
    public List<PlaceCardResponse> getTopPlaceCards() {
        return toPlaceCards(getTopPlaces());
    }

    @Transactional(readOnly = true)
    public List<Place> getTrendingPlaces() {
        return sortPlaces(placeRepository.findAllByIsTrendingTrueOrderByTiktokTrendScoreDesc());
    }

    @Transactional(readOnly = true)
    public List<PlaceCardResponse> getTrendingPlaceCards() {
        return toPlaceCards(getTrendingPlaces());
    }

    @Transactional(readOnly = true)
    public List<Place> searchPlaces(Integer cityId, String category, String vibeTag, Boolean trendingOnly) {
        List<Place> places;

        if (cityId != null && category != null && !category.isBlank()) {
            places = placeRepository.findAllByCityIdAndCategoryIgnoreCase(cityId, category.trim());
        } else if (cityId != null && vibeTag != null && !vibeTag.isBlank()) {
            places = placeRepository.findAllByCityIdAndVibeTagIgnoreCase(cityId, vibeTag.trim());
        } else if (cityId != null) {
            places = placeRepository.findAllByCityIdOrderBySmartScoreDesc(cityId);
        } else {
            places = placeRepository.findAll();
        }

        return sortPlaces(places.stream()
                .filter(place -> trendingOnly == null || !trendingOnly || Boolean.TRUE.equals(place.getIsTrending()))
                .filter(place -> category == null || category.isBlank() || hasValue(place.getCategory(), category))
                .filter(place -> vibeTag == null || vibeTag.isBlank() || hasValue(place.getVibeTag(), vibeTag))
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<PlaceCardResponse> searchPlaceCards(Integer cityId, String category, String vibeTag, Boolean trendingOnly) {
        return toPlaceCards(searchPlaces(cityId, category, vibeTag, trendingOnly));
    }

    @Transactional(readOnly = true)
    public List<PlaceCardResponse> toPlaceCards(List<Place> places) {
        return sortPlaces(places).stream()
                .map(this::toPlaceCard)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlaceCardResponse toPlaceCard(Place place) {
        return new PlaceCardResponse(
                place.getId(),
                place.getName(),
                place.getDescription(),
                place.getCategory(),
                place.getVibeTag(),
                place.getCity() != null ? place.getCity().getName() : null,
                place.getGoogleRating(),
                place.getTiktokTrendScore(),
                place.getSmartScore(),
                place.getIsTrending(),
                place.getTrendReason(),
                place.getImageUrl(),
                buildMapsUrl(place),
                place.getLatitude(),
                place.getLongitude(),
                place.getOperatingHours() != null ? place.getOperatingHours().size() : 0,
                place.getMediaList() != null ? place.getMediaList().size() : 0,
                buildPlaceSubtitle(place),
                buildScoreLabel(place),
                buildTrendLabel(place),
                buildBadges(place),
                buildMapsUrl(place)
        );
    }

    private boolean hasValue(String actualValue, String requestedValue) {
        return actualValue != null && actualValue.toLowerCase(Locale.ROOT).equals(requestedValue.trim().toLowerCase(Locale.ROOT));
    }

    private List<Place> sortPlaces(List<Place> places) {
        return places.stream()
                .sorted(Comparator
                        .comparing(Place::getSmartScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Place::getGoogleRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Place::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    private void normalizePlaceForSave(Place place) {
        place.setCity(resolveCity(place));
        normalizeScores(place);
        synchronizeOperatingHours(place);
        synchronizeMediaList(place);

        if ((place.getGoogleMapsUrl() == null || place.getGoogleMapsUrl().isBlank())
                && place.getLatitude() != null && place.getLongitude() != null) {
            place.setGoogleMapsUrl(buildMapsUrl(place));
        }
    }

    private void normalizeScores(Place place) {
        if (place.getGoogleRating() == null) {
            place.setGoogleRating(0.0);
        }

        if (place.getTiktokTrendScore() == null) {
            place.setTiktokTrendScore(0.0);
        }

        place.setSmartScore(place.getGoogleRating() + place.getTiktokTrendScore());

        if (place.getIsTrending() == null) {
            place.setIsTrending(place.getTiktokTrendScore() > 20);
        }
    }

    private void mergeIntoExistingPlace(Place target, Place source) {
        if (source.getName() != null) {
            target.setName(source.getName());
        }
        if (source.getDescription() != null) {
            target.setDescription(source.getDescription());
        }
        if (source.getCategory() != null) {
            target.setCategory(source.getCategory());
        }
        if (source.getVibeTag() != null) {
            target.setVibeTag(source.getVibeTag());
        }
        if (source.getLatitude() != null) {
            target.setLatitude(source.getLatitude());
        }
        if (source.getLongitude() != null) {
            target.setLongitude(source.getLongitude());
        }
        if (source.getGoogleRating() != null) {
            target.setGoogleRating(source.getGoogleRating());
        }
        if (source.getGoogleMapsUrl() != null) {
            target.setGoogleMapsUrl(source.getGoogleMapsUrl());
        }
        if (source.getTiktokTrendScore() != null) {
            target.setTiktokTrendScore(source.getTiktokTrendScore());
        }
        if (source.getIsTrending() != null) {
            target.setIsTrending(source.getIsTrending());
        }
        if (source.getSmartScore() != null) {
            target.setSmartScore(source.getSmartScore());
        }
        if (source.getImageUrl() != null) {
            target.setImageUrl(source.getImageUrl());
        }
        if (source.getTiktokVideoUrl() != null) {
            target.setTiktokVideoUrl(source.getTiktokVideoUrl());
        }
        if (source.getTrendReason() != null) {
            target.setTrendReason(source.getTrendReason());
        }

        if (source.getCity() != null) {
            target.setCity(source.getCity());
        }

        if (source.getOperatingHours() != null) {
            replaceOperatingHours(target, source.getOperatingHours());
        }

        if (source.getMediaList() != null) {
            replaceMediaList(target, source.getMediaList());
        }
    }

    private City resolveCity(Place place) {
        if (place.getCity() == null) {
            throw new ApiException("City is required");
        }

        if (place.getCity().getId() != null) {
            return cityRepository.findById(place.getCity().getId())
                    .orElseThrow(() -> new ApiException("City not found"));
        }

        if (place.getCity().getName() != null && !place.getCity().getName().isBlank()) {
            City city = cityRepository.findCityByName(place.getCity().getName());
            if (city != null) {
                return city;
            }
        }

        throw new ApiException("City not found");
    }

    private Place toPlace(PlaceRequest request) {
        Place place = new Place();
        applyRequestToPlace(place, request);
        return place;
    }

    private void applyRequestToPlace(Place place, PlaceRequest request) {
        place.setName(request.getName());
        place.setDescription(request.getDescription());
        place.setCategory(request.getCategory());
        place.setVibeTag(request.getVibeTag());
        place.setLatitude(request.getLatitude());
        place.setLongitude(request.getLongitude());
        place.setGoogleRating(request.getGoogleRating());
        place.setGoogleMapsUrl(request.getGoogleMapsUrl());
        place.setImageUrl(request.getImageUrl());
        place.setTiktokVideoUrl(request.getTiktokVideoUrl());
        place.setTrendReason(request.getTrendReason());

        if (request.getCityId() != null) {
            place.setCity(toCityReference(request.getCityId()));
        }

        if (request.getOperatingHours() != null) {
            replaceOperatingHours(place, toOperatingHours(request.getOperatingHours(), place));
        }

        if (request.getMediaList() != null) {
            replaceMediaList(place, toPlaceMedia(request, place));
        }
    }

    private City toCityReference(Integer cityId) {
        City city = new City();
        city.setId(cityId);
        return city;
    }

    private List<OperatingHours> toOperatingHours(List<OperatingHoursRequest> requests, Place place) {
        if (requests == null) {
            return null;
        }

        return requests.stream()
                .map(request -> toOperatingHoursEntity(request, place))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<PlaceMedia> toPlaceMedia(PlaceRequest request, Place place) {
        if (request.getMediaList() == null) {
            return null;
        }

        return placeMediaService.toEntities(request.getMediaList(), place);
    }

    private void synchronizeOperatingHours(Place place) {
        List<OperatingHours> normalizedHours = operatingHoursService.normalizeForPlace(place);
        replaceOperatingHours(place, normalizedHours);
    }

    private void synchronizeMediaList(Place place) {
        List<PlaceMedia> normalizedMedia = placeMediaService.normalizeForPlace(place);
        replaceMediaList(place, normalizedMedia);
    }

    private void replaceOperatingHours(Place place, List<OperatingHours> operatingHours) {
        if (place.getOperatingHours() == null) {
            place.setOperatingHours(operatingHours == null ? null : new ArrayList<>(operatingHours));
            return;
        }

        place.getOperatingHours().clear();
        if (operatingHours != null) {
            place.getOperatingHours().addAll(operatingHours);
        }
    }

    private void replaceMediaList(Place place, List<PlaceMedia> mediaList) {
        if (place.getMediaList() == null) {
            place.setMediaList(mediaList == null ? null : new ArrayList<>(mediaList));
            return;
        }

        place.getMediaList().clear();
        if (mediaList != null) {
            place.getMediaList().addAll(mediaList);
        }
    }

    private OperatingHours toOperatingHoursEntity(OperatingHoursRequest request, Place place) {
        OperatingHours hours = new OperatingHours();
        hours.setDayOfWeek(request.getDayOfWeek());
        hours.setOpenTime(request.getOpenTime());
        hours.setCloseTime(request.getCloseTime());
        hours.setIsSplitShift(request.getSplitShift());
        hours.setSecondOpen(request.getSecondOpen());
        hours.setSecondClose(request.getSecondClose());
        hours.setPrayerBreak(request.getPrayerBreak());
        hours.setNotes(request.getNotes());
        hours.setPlace(place);
        return hours;
    }

    private void addTag(List<String> tags, String value) {
        if (value != null && !value.isBlank()) {
            tags.add(value);
        }
    }

    private List<String> buildBadges(Place place) {
        Set<String> badges = new LinkedHashSet<>();
        addBadge(badges, place.getCategory());
        addBadge(badges, place.getVibeTag());
        if (place.getCity() != null) {
            addBadge(badges, place.getCity().getName());
        }
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
        badges.add(capitalizeWords(value.replace('_', ' ')));
    }

    private String buildPlaceSubtitle(Place place) {
        List<String> parts = new ArrayList<>();
        if (place.getCity() != null && place.getCity().getName() != null) {
            parts.add(place.getCity().getName());
        }
        if (place.getCategory() != null) {
            parts.add(capitalizeWords(place.getCategory().replace('_', ' ')));
        }
        if (place.getVibeTag() != null) {
            parts.add(capitalizeWords(place.getVibeTag()));
        }
        return parts.isEmpty() ? "Place card" : String.join(" • ", parts);
    }

    private String buildScoreLabel(Place place) {
        double googleRating = place.getGoogleRating() != null ? place.getGoogleRating() : 0.0;
        double tiktokTrendScore = place.getTiktokTrendScore() != null ? place.getTiktokTrendScore() : 0.0;
        double smartScore = place.getSmartScore() != null ? place.getSmartScore() : googleRating + tiktokTrendScore;
        return "Google " + formatOneDecimal(googleRating)
                + " • TikTok " + formatOneDecimal(tiktokTrendScore)
                + " • Smart " + formatOneDecimal(smartScore);
    }

    private String buildTrendLabel(Place place) {
        if (Boolean.TRUE.equals(place.getIsTrending())) {
            return "Trending on TikTok";
        }
        if (place.getTiktokTrendScore() != null && place.getTiktokTrendScore() > 0) {
            return "Has TikTok signal";
        }
        return "Google-led recommendation";
    }

    private String buildHoursSummary(List<OperatingHours> operatingHours) {
        if (operatingHours == null || operatingHours.isEmpty()) {
            return "Operating hours not added yet";
        }
        return "Operating hours available for " + operatingHours.size() + " day(s)";
    }

    private String buildHeroMediaUrl(Place place, List<PlaceMedia> mediaItems) {
        if (place.getImageUrl() != null && !place.getImageUrl().isBlank()) {
            return place.getImageUrl();
        }
        if (place.getTiktokVideoUrl() != null && !place.getTiktokVideoUrl().isBlank()) {
            return place.getTiktokVideoUrl();
        }
        if (mediaItems != null && !mediaItems.isEmpty()) {
            PlaceMedia media = mediaItems.get(0);
            if (media.getThumbnail() != null && !media.getThumbnail().isBlank()) {
                return media.getThumbnail();
            }
            return media.getUrl();
        }
        return null;
    }

    private String buildRecommendationSummary(Place place) {
        if (Boolean.TRUE.equals(place.getIsTrending())) {
            return "Strong fit for the trending discoveries section and replace-stop suggestions.";
        }
        if (place.getGoogleRating() != null && place.getGoogleRating() >= 4.5) {
            return "High Google rating makes this a solid recommendation even without a strong TikTok push.";
        }
        if (place.getVibeTag() != null && !place.getVibeTag().isBlank()) {
            return "Useful for users looking for a " + place.getVibeTag().trim() + " experience.";
        }
        return "Available as a supporting place option for itinerary planning.";
    }

    private String buildMapsUrl(Place place) {
        if (place.getGoogleMapsUrl() != null && !place.getGoogleMapsUrl().isBlank()) {
            return place.getGoogleMapsUrl();
        }

        if (place.getLatitude() != null && place.getLongitude() != null) {
            return "https://www.google.com/maps/search/?api=1&query=" + place.getLatitude() + "," + place.getLongitude();
        }

        return buildSearchUrl(place.getName() + " location");
    }

    private String buildSearchUrl(String query) {
        return "https://www.google.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    }

    private String formatOneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
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
}
