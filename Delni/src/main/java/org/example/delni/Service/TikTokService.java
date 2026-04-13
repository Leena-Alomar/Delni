package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiException;
import org.example.delni.DTO.External.OpenAiTikTokResultCandidate;
import org.example.delni.DTO.External.OpenAiTikTokResultReview;
import org.example.delni.DTO.External.OpenAiTikTokSearchPlan;
import org.example.delni.DTO.External.TikTokResponse;
import org.example.delni.DTO.Out.TikTokMediaPreviewResponse;
import org.example.delni.DTO.Out.TrendRefreshResponse;
import org.example.delni.Model.Place;
import org.example.delni.Repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TikTokService {

    private static final Logger log = LoggerFactory.getLogger(TikTokService.class);
    private static final Map<String, String> CITY_ARABIC_MAP = Map.ofEntries(
            Map.entry("riyadh", "الرياض"),
            Map.entry("jeddah", "جدة"),
            Map.entry("yanbu", "ينبع"),
            Map.entry("alula", "العلا"),
            Map.entry("al ula", "العلا"),
            Map.entry("khobar", "الخبر"),
            Map.entry("dammam", "الدمام"),
            Map.entry("abha", "أبها"),
            Map.entry("taif", "الطائف"),
            Map.entry("makkah", "مكة"),
            Map.entry("mecca", "مكة"),
            Map.entry("madinah", "المدينة"),
            Map.entry("medina", "المدينة")
    );
    private static final Map<String, String> CATEGORY_ARABIC_MAP = Map.ofEntries(
            Map.entry("cafe", "كافيه"),
            Map.entry("restaurant", "مطعم"),
            Map.entry("museum", "متحف"),
            Map.entry("shopping", "مول"),
            Map.entry("shopping_mall", "مول"),
            Map.entry("entertainment", "فعاليات"),
            Map.entry("tourist_attraction", "معلم سياحي"),
            Map.entry("park", "حديقة"),
            Map.entry("beach", "شاطئ"),
            Map.entry("nature", "طبيعة"),
            Map.entry("adventure", "مغامرات")
    );

    @Value("${tiktok.api.key}")
    private String apiKey;

    @Value("${tiktok.trend.window-days:14}")
    private int trendWindowDays;

    @Value("${tiktok.trend.min-recent-videos:3}")
    private int minRecentVideos;

    @Value("${tiktok.trend.min-local-videos:2}")
    private int minLocalVideos;

    private final PlaceRepository placeRepository;
    private final PlaceMediaService placeMediaService;
    private final OpenAiTikTokSearchService openAiTikTokSearchService;
    private final RestTemplate restTemplate;

    public double fetchTrendScore(String keyword) {
        TrendLookupResult result = lookupTrend(null, keyword);
        return result.successful() ? result.score() : -1;
    }

    public void updatePlaceTrend(Place place) {
        updatePlaceTrendState(place);
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public TrendRefreshResponse updateAllTrends() {
        List<TrendUpdateState> states = placeRepository.findAll().stream()
                .map(this::updatePlaceTrendState)
                .sorted(Comparator
                        .comparing((TrendUpdateState state) -> state.result().getTiktokTrendScore(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(state -> state.result().getGoogleRating(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(state -> state.result().getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return buildRefreshResponse(states);
    }

    public TrendRefreshResponse updateTrendForPlace(Integer placeId) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new ApiException("Place not found"));
        return buildRefreshResponse(List.of(updatePlaceTrendState(place)));
    }

    private TrendRefreshResponse buildRefreshResponse(List<TrendUpdateState> states) {
        int totalPlaces = states.size();
        int updatedPlaces = (int) states.stream().filter(TrendUpdateState::updated).count();
        int skippedPlaces = (int) states.stream().filter(TrendUpdateState::skipped).count();
        int noResultPlaces = totalPlaces - updatedPlaces - skippedPlaces;
        int trendingPlaces = (int) states.stream()
                .filter(state -> Boolean.TRUE.equals(state.result().getTrending()))
                .count();

        String status;
        String message;
        String summaryTitle;
        String summarySubtitle;

        if (totalPlaces == 0) {
            status = "empty";
            message = "No places found to refresh.";
            summaryTitle = "No places available";
            summarySubtitle = "Add or sync places first, then refresh TikTok trends.";
        } else if (!hasApiKey()) {
            status = "skipped";
            message = "TikTok refresh skipped because RAPIDAPI_KEY is not configured.";
            summaryTitle = "TikTok refresh skipped";
            summarySubtitle = "Add the RapidAPI key to enable AI-guided TikTok search.";
        } else {
            status = "completed";
            message = "TikTok refresh completed for " + totalPlaces + " place(s).";
            summaryTitle = "TikTok refresh completed";
            summarySubtitle = updatedPlaces + " place(s) updated, " + trendingPlaces + " marked trending, "
                    + noResultPlaces + " still without a strong TikTok match.";
        }

        return new TrendRefreshResponse(
                status,
                summaryTitle,
                summarySubtitle,
                openAiTikTokSearchService.getStrategySummary(),
                buildGlobalAiPrompt(),
                totalPlaces,
                updatedPlaces,
                trendingPlaces,
                noResultPlaces,
                skippedPlaces,
                message,
                states.stream()
                        .map(TrendUpdateState::result)
                        .toList()
        );
    }

    private TrendUpdateState updatePlaceTrendState(Place place) {
        if (place == null) {
            return new TrendUpdateState(false, true, null);
        }

        double existingTrendScore = place.getTiktokTrendScore() != null ? place.getTiktokTrendScore() : 0.0;
        double googleRating = place.getGoogleRating() != null ? place.getGoogleRating() : 0.0;
        List<String> heuristicKeywords = buildSearchKeywords(place);
        OpenAiTikTokSearchPlan searchPlan = openAiTikTokSearchService.generateSearchPlan(place, heuristicKeywords);
        List<String> attemptedKeywords = searchPlan.getSearchKeywords();
        String searchPrompt = buildSearchPrompt(place, attemptedKeywords);

        if (!hasApiKey()) {
            place.setTiktokTrendScore(existingTrendScore);
            place.setIsTrending(Boolean.TRUE.equals(place.getIsTrending()));
            place.setSmartScore(googleRating + existingTrendScore);
            if (place.getTrendReason() == null || place.getTrendReason().isBlank()) {
                place.setTrendReason("TikTok refresh skipped because RAPIDAPI_KEY is not configured.");
            }
            placeRepository.save(place);
            List<TikTokMediaPreviewResponse> syncedPreviews = List.of();
            return new TrendUpdateState(
                    false,
                    true,
                    toTrendPlaceResult(place, "skipped", null, attemptedKeywords, searchPrompt, null, syncedPreviews)
            );
        }

        TrendLookupResult trendLookup = lookupBestTrend(place, attemptedKeywords);
        if (!trendLookup.successful()) {
            place.setTiktokTrendScore(existingTrendScore);
            place.setIsTrending(Boolean.TRUE.equals(place.getIsTrending()));
            place.setSmartScore(googleRating + existingTrendScore);
            String reviewReason = trendLookup.reviewReasoning() != null && !trendLookup.reviewReasoning().isBlank()
                    ? " " + trendLookup.reviewReasoning()
                    : "";
            place.setTrendReason("No clear TikTok match yet. Tried: " + String.join(", ", attemptedKeywords) + reviewReason);
            placeRepository.save(place);
            placeMediaService.syncTikTokMedia(place, List.of());
            return new TrendUpdateState(
                    false,
                    false,
                    toTrendPlaceResult(place, "no_match", null, attemptedKeywords, searchPrompt, null, List.of())
            );
        }

        double trendScore = trendLookup.score();
        place.setTiktokTrendScore(trendScore);
        String reviewReason = trendLookup.reviewReasoning() != null && !trendLookup.reviewReasoning().isBlank()
                ? " " + trendLookup.reviewReasoning()
                : "";
        place.setTrendReason("Matched TikTok search \"" + trendLookup.keyword() + "\" using "
                + trendLookup.matchedItems() + " result(s), "
                + trendLookup.recentVideoCount() + " recent video(s), and "
                + trendLookup.recentLocalVideoCount() + " recent local video(s)." + reviewReason);
        place.setIsTrending(trendLookup.trending());
        place.setSmartScore(googleRating + trendScore);
        if (!trendLookup.mediaPreviews().isEmpty()) {
            TikTokMediaPreviewResponse leadPreview = trendLookup.mediaPreviews().get(0);
            if (leadPreview.getVideoUrl() != null && !leadPreview.getVideoUrl().isBlank()) {
                place.setTiktokVideoUrl(leadPreview.getVideoUrl());
            }
            if ((place.getImageUrl() == null || place.getImageUrl().isBlank())
                    && leadPreview.getThumbnailUrl() != null && !leadPreview.getThumbnailUrl().isBlank()) {
                place.setImageUrl(leadPreview.getThumbnailUrl());
            }
        }
        placeRepository.save(place);
        List<TikTokMediaPreviewResponse> syncedPreviews = placeMediaService.syncTikTokMedia(place, trendLookup.mediaPreviews());

        return new TrendUpdateState(
                true,
                false,
                toTrendPlaceResult(place, "updated", trendLookup.keyword(), attemptedKeywords, searchPrompt, trendLookup, syncedPreviews)
        );
    }

    private TrendLookupResult lookupBestTrend(Place place, List<String> keywords) {
        List<TrendLookupResult> candidates = new ArrayList<>();

        for (String keyword : keywords) {
            TrendLookupResult result = lookupTrend(place, keyword);
            if (result.successful()) {
                candidates.add(result);
            }
        }

        if (candidates.isEmpty()) {
            return TrendLookupResult.notFound();
        }

        TrendLookupResult highestScoreCandidate = candidates.stream()
                .max(Comparator.comparing(TrendLookupResult::score))
                .orElse(TrendLookupResult.notFound());

        if (!openAiTikTokSearchService.isEnabled()) {
            return highestScoreCandidate;
        }

        OpenAiTikTokResultReview review = openAiTikTokSearchService.reviewCandidates(
                place,
                candidates.stream()
                        .map(this::toResultCandidate)
                        .toList()
        );

        if (review == null) {
            return highestScoreCandidate;
        }

        double relevanceScore = review.getRelevanceScore() != null ? review.getRelevanceScore() : 0.0;
        if ("no_match".equalsIgnoreCase(review.getDecision()) || relevanceScore < 0.35) {
            return TrendLookupResult.notFound(review.getReasoning());
        }

        TrendLookupResult reviewedCandidate = candidates.stream()
                .filter(candidate -> candidate.keyword() != null
                        && candidate.keyword().equalsIgnoreCase(review.getBestKeyword()))
                .findFirst()
                .orElse(highestScoreCandidate);

        double adjustedScore = reviewedCandidate.score() * Math.max(0.5, relevanceScore);
        return TrendLookupResult.found(
                adjustedScore,
                reviewedCandidate.keyword(),
                reviewedCandidate.matchedItems(),
                reviewedCandidate.recentVideoCount(),
                reviewedCandidate.localVideoCount(),
                reviewedCandidate.recentLocalVideoCount(),
                reviewedCandidate.trending() && relevanceScore >= 0.5,
                reviewedCandidate.mediaPreviews(),
                review.getReasoning()
        );
    }

    private TrendLookupResult lookupTrend(Place place, String keyword) {
        if (!hasApiKey() || keyword == null || keyword.isBlank()) {
            return TrendLookupResult.notFound();
        }

        String url = UriComponentsBuilder
                .fromUriString("https://tiktok-api23.p.rapidapi.com/api/search/general")
                .queryParam("keyword", keyword)
                .queryParam("cursor", 0)
                .queryParam("search_id", 0)
                .encode()
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", apiKey);
        headers.set("x-rapidapi-host", "tiktok-api23.p.rapidapi.com");

        try {
            ResponseEntity<TikTokResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    TikTokResponse.class
            );

            TikTokResponse body = response.getBody();
            double score = 0;
            int matchedItems = 0;
            int recentVideoCount = 0;
            int localVideoCount = 0;
            int recentLocalVideoCount = 0;
            List<TikTokMediaPreviewResponse> mediaPreviews = new ArrayList<>();

            if (body != null && body.getData() != null) {
                for (TikTokResponse.Entry video : body.getData()) {
                    TikTokResponse.Stats stats = extractStats(video);
                    if (stats == null) {
                        continue;
                    }

                    long views = stats.getPlayCount();
                    long likes = stats.getDiggCount();
                    TikTokResponse.Item item = video.getItem();
                    boolean recent = item != null && isRecent(item.getCreateTime());
                    boolean local = item != null && isLocalSignal(place, item);

                    if (recent) {
                        recentVideoCount++;
                    }
                    if (local) {
                        localVideoCount++;
                    }
                    if (recent && local) {
                        recentLocalVideoCount++;
                    }

                    score += (views * 0.00001) + (likes * 0.001);
                    if (recent) {
                        score += 8.0;
                    }
                    if (local) {
                        score += 10.0;
                    }
                    if (recent && local) {
                        score += 15.0;
                    }
                    matchedItems++;
                    if (item != null && mediaPreviews.size() < 3) {
                        TikTokMediaPreviewResponse preview = toMediaPreview(item, stats, recent);
                        if (preview.getVideoUrl() != null && !preview.getVideoUrl().isBlank()) {
                            mediaPreviews.add(preview);
                        }
                    }
                }
            }

            boolean trending = recentVideoCount >= minRecentVideos
                    && (localVideoCount >= minLocalVideos || recentLocalVideoCount >= 1);

            return matchedItems > 0
                    ? TrendLookupResult.found(
                    score,
                    keyword,
                    matchedItems,
                    recentVideoCount,
                    localVideoCount,
                    recentLocalVideoCount,
                    trending,
                    mediaPreviews,
                    null)
                    : TrendLookupResult.notFound();
        } catch (Exception exception) {
            log.warn("TikTok lookup failed for keyword {}: {}", keyword, exception.getMessage());
            return TrendLookupResult.notFound();
        }
    }

    private List<String> buildSearchKeywords(Place place) {
        Set<String> keywords = new LinkedHashSet<>();
        String cityName = place.getCity() != null ? place.getCity().getName() : null;
        String category = normalizeToken(place.getCategory());
        String vibeTag = normalizeToken(place.getVibeTag());
        String arabicCity = translateCityToArabic(cityName);
        String arabicCategory = translateCategoryToArabic(category);

        addKeyword(keywords, place.getName());
        addKeyword(keywords, simplifyName(place.getName()));
        addKeyword(keywords, joinTokens(place.getName(), cityName));
        addKeyword(keywords, joinTokens(cityName, place.getName()));
        addKeyword(keywords, joinTokens(place.getName(), category));
        addKeyword(keywords, joinTokens(place.getName(), vibeTag));
        addKeyword(keywords, joinTokens("best", category, cityName));
        addKeyword(keywords, joinTokens("viral", category, cityName));
        addKeyword(keywords, joinTokens("top", category, cityName));
        addKeyword(keywords, joinTokens("review", place.getName(), cityName));
        addKeyword(keywords, joinTokens("recommendation", category, cityName));
        addKeyword(keywords, joinTokens(place.getName(), "Saudi"));
        addKeyword(keywords, joinTokens(place.getName(), "Saudi Arabia"));
        addKeyword(keywords, joinTokens(place.getName(), "السعودية"));
        addKeyword(keywords, joinTokens(arabicCategory, arabicCity));
        addKeyword(keywords, joinTokens("ترند", arabicCategory, arabicCity));
        addKeyword(keywords, joinTokens("افضل", arabicCategory, arabicCity));
        addKeyword(keywords, joinTokens(place.getName(), arabicCity));

        return keywords.stream()
                .limit(10)
                .toList();
    }

    private String buildSearchPrompt(Place place, List<String> keywords) {
        String cityName = place.getCity() != null ? place.getCity().getName() : "the selected city";
        String category = place.getCategory() != null ? place.getCategory() : "place";
        String vibeTag = place.getVibeTag() != null ? place.getVibeTag() : "general";

        return "Search TikTok for travel-worthy videos and user recommendations about "
                + place.getName() + " in " + cityName
                + ". Start with the exact place name, then try category, vibe, and English/Arabic discovery phrases. "
                + "Treat it as a " + vibeTag + " " + category + " recommendation flow. Keywords: "
                + String.join(" | ", keywords);
    }

    private String buildGlobalAiPrompt() {
        return openAiTikTokSearchService.getPromptSummary();
    }

    private String buildScoreSummary(Place place) {
        double googleRating = place.getGoogleRating() != null ? place.getGoogleRating() : 0.0;
        double tiktokTrendScore = place.getTiktokTrendScore() != null ? place.getTiktokTrendScore() : 0.0;
        double smartScore = place.getSmartScore() != null ? place.getSmartScore() : googleRating + tiktokTrendScore;
        return "Google " + formatOneDecimal(googleRating)
                + " + TikTok " + formatOneDecimal(tiktokTrendScore)
                + " = Smart " + formatOneDecimal(smartScore);
    }

    private String buildRecommendation(Place place, TrendLookupResult trendLookup) {
        double trendScore = place.getTiktokTrendScore() != null ? place.getTiktokTrendScore() : 0.0;
        double googleRating = place.getGoogleRating() != null ? place.getGoogleRating() : 0.0;

        if (trendLookup != null && trendLookup.trending()) {
            return "Strong recent TikTok signal in the last " + trendWindowDays
                    + " days. Good fit for trending cards and trip highlights.";
        }

        if (trendLookup != null && trendLookup.recentVideoCount() > 0) {
            return "Recent TikTok activity exists, but it is not strong enough yet to mark this place as trending.";
        }

        if (trendScore > 0) {
            return "Moderate TikTok signal. Keep this as a supporting recommendation rather than a lead trend pick.";
        }

        if (googleRating >= 4.5) {
            return "No strong TikTok signal yet, but the Google rating still makes it a good backup recommendation.";
        }

        return "Needs better keyword coverage or manual review before promoting it as a trending discovery.";
    }

    private TrendRefreshResponse.TrendPlaceResult toTrendPlaceResult(
            Place place,
            String status,
            String matchedKeyword,
            List<String> attemptedKeywords,
            String searchPrompt,
            TrendLookupResult trendLookup,
            List<TikTokMediaPreviewResponse> mediaPreviews
    ) {
        return new TrendRefreshResponse.TrendPlaceResult(
                place.getId(),
                place.getName(),
                place.getCity() != null ? place.getCity().getName() : null,
                place.getGoogleRating(),
                place.getTiktokTrendScore(),
                place.getSmartScore(),
                place.getIsTrending(),
                place.getTrendReason(),
                status,
                matchedKeyword,
                attemptedKeywords,
                searchPrompt,
                buildScoreSummary(place),
                buildRecommendation(place, trendLookup),
                trendLookup != null ? trendLookup.matchedItems() : 0,
                trendLookup != null ? trendLookup.recentVideoCount() : 0,
                trendLookup != null ? trendLookup.localVideoCount() : 0,
                trendLookup != null ? trendLookup.recentLocalVideoCount() : 0,
                trendWindowDays,
                mediaPreviews
        );
    }

    private TikTokMediaPreviewResponse toMediaPreview(TikTokResponse.Item item, TikTokResponse.Stats stats, boolean recent) {
        String creatorHandle = item.getAuthor() != null && item.getAuthor().getUniqueId() != null
                ? "@" + item.getAuthor().getUniqueId()
                : null;
        String creatorName = item.getAuthor() != null ? item.getAuthor().getNickname() : null;
        String thumbnailUrl = null;
        String videoUrl = null;

        if (item.getVideo() != null) {
            thumbnailUrl = item.getVideo().getDynamicCover() != null && !item.getVideo().getDynamicCover().isBlank()
                    ? item.getVideo().getDynamicCover()
                    : item.getVideo().getCover();
            videoUrl = firstNonBlank(
                    item.getVideo().getPlayAddr(),
                    item.getVideo().getPlayAddrH264(),
                    item.getVideo().getPlayAddrBytevc1(),
                    firstFromList(item.getVideo().getPlayAddrUrlList()),
                    item.getVideo().getDownloadAddr()
            );
        }

        return new TikTokMediaPreviewResponse(
                item.getId(),
                videoUrl,
                thumbnailUrl,
                creatorName,
                creatorHandle,
                item.getDesc(),
                stats != null ? stats.getPlayCount() : null,
                stats != null ? stats.getDiggCount() : null,
                item.getCreateTime(),
                recent
        );
    }

    private OpenAiTikTokResultCandidate toResultCandidate(TrendLookupResult result) {
        return new OpenAiTikTokResultCandidate(
                result.keyword(),
                result.score(),
                result.matchedItems(),
                result.recentVideoCount(),
                result.localVideoCount(),
                result.recentLocalVideoCount(),
                result.mediaPreviews().stream()
                        .map(TikTokMediaPreviewResponse::getCaption)
                        .filter(caption -> caption != null && !caption.isBlank())
                        .limit(3)
                        .toList()
        );
    }

    private boolean isRecent(Long createTime) {
        if (createTime == null || createTime <= 0) {
            return false;
        }

        long epochMillis = createTime > 1_000_000_000_000L ? createTime : createTime * 1000;
        Instant createdAt = Instant.ofEpochMilli(epochMillis);
        Instant cutoff = Instant.now().minus(trendWindowDays, ChronoUnit.DAYS);
        return !createdAt.isBefore(cutoff);
    }

    private boolean isLocalSignal(Place place, TikTokResponse.Item item) {
        Set<String> signals = new LinkedHashSet<>();
        addSignal(signals, item.getDesc());
        if (item.getAuthor() != null) {
            addSignal(signals, item.getAuthor().getNickname());
            addSignal(signals, item.getAuthor().getUniqueId());
        }

        String combined = String.join(" ", signals).toLowerCase(Locale.ROOT);
        String cityName = place != null && place.getCity() != null ? place.getCity().getName() : null;
        String normalizedCity = cityName == null ? null : cityName.trim().toLowerCase(Locale.ROOT);
        String arabicCity = translateCityToArabic(cityName);

        boolean saudiSignal = combined.contains("saudi")
                || combined.contains("ksa")
                || combined.contains("السعود")
                || combined.contains("المملكة");

        boolean citySignal = normalizedCity != null && !normalizedCity.isBlank() && combined.contains(normalizedCity);
        boolean arabicCitySignal = arabicCity != null
                && !arabicCity.isBlank()
                && combined.contains(arabicCity.toLowerCase(Locale.ROOT));

        return saudiSignal || citySignal || arabicCitySignal;
    }

    private void addSignal(Set<String> signals, String value) {
        if (value != null && !value.isBlank()) {
            signals.add(value);
        }
    }

    private String firstFromList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void addKeyword(Set<String> keywords, String value) {
        if (value == null) {
            return;
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (!normalized.isBlank()) {
            keywords.add(normalized);
        }
    }

    private String simplifyName(String name) {
        if (name == null) {
            return null;
        }

        String simplified = name.trim().replaceAll("\\s+", " ");
        int splitIndex = simplified.indexOf(' ');
        if (splitIndex < 0) {
            return simplified;
        }

        String firstToken = simplified.substring(0, splitIndex).trim();
        return firstToken.isBlank() ? simplified : firstToken;
    }

    private String translateCityToArabic(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return null;
        }

        return CITY_ARABIC_MAP.getOrDefault(cityName.trim().toLowerCase(Locale.ROOT), cityName);
    }

    private String translateCategoryToArabic(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        return CATEGORY_ARABIC_MAP.getOrDefault(category.trim().toLowerCase(Locale.ROOT), category);
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().replace('_', ' ');
    }

    private String joinTokens(String... parts) {
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }

            String normalized = part.trim().replaceAll("\\s+", " ");
            if (!normalized.isBlank()) {
                tokens.add(normalized);
            }
        }

        return tokens.isEmpty() ? null : String.join(" ", tokens);
    }

    private String formatOneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private TikTokResponse.Stats extractStats(TikTokResponse.Entry entry) {
        if (entry == null) {
            return null;
        }

        if (entry.getStats() != null) {
            return entry.getStats();
        }

        if (entry.getItem() != null) {
            return entry.getItem().getStats();
        }

        return null;
    }

    private record TrendLookupResult(boolean successful, double score, String keyword, int matchedItems,
                                     int recentVideoCount, int localVideoCount, int recentLocalVideoCount,
                                     boolean trending, List<TikTokMediaPreviewResponse> mediaPreviews,
                                     String reviewReasoning) {
        private static TrendLookupResult found(double score, String keyword, int matchedItems,
                                               int recentVideoCount, int localVideoCount, int recentLocalVideoCount,
                                               boolean trending, List<TikTokMediaPreviewResponse> mediaPreviews,
                                               String reviewReasoning) {
            return new TrendLookupResult(
                    true,
                    score,
                    keyword,
                    matchedItems,
                    recentVideoCount,
                    localVideoCount,
                    recentLocalVideoCount,
                    trending,
                    mediaPreviews,
                    reviewReasoning
            );
        }

        private static TrendLookupResult notFound() {
            return new TrendLookupResult(false, -1, null, 0, 0, 0, 0, false, List.of(), null);
        }

        private static TrendLookupResult notFound(String reviewReasoning) {
            return new TrendLookupResult(false, -1, null, 0, 0, 0, 0, false, List.of(), reviewReasoning);
        }
    }

    private record TrendUpdateState(boolean updated, boolean skipped, TrendRefreshResponse.TrendPlaceResult result) {
    }
}
