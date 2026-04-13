package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.DTO.In.PlaceMediaRequest;
import org.example.delni.DTO.Out.TikTokMediaPreviewResponse;
import org.example.delni.Model.Place;
import org.example.delni.Model.PlaceMedia;
import org.example.delni.Repository.PlaceMediaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlaceMediaService {

    private final PlaceMediaRepository placeMediaRepository;

    public List<PlaceMedia> getByPlaceId(Integer placeId) {
        return new ArrayList<>(placeMediaRepository.findAllByPlaceId(placeId));
    }

    public List<PlaceMedia> toEntities(List<PlaceMediaRequest> mediaRequests, Place place) {
        List<PlaceMedia> normalizedMedia = mediaRequests == null
                ? new ArrayList<>()
                : mediaRequests.stream()
                .map(request -> toEntity(request, place))
                .collect(Collectors.toCollection(ArrayList::new));

        appendTikTokMedia(normalizedMedia, place);
        return normalizedMedia;
    }

    public List<PlaceMedia> normalizeForPlace(Place place) {
        List<PlaceMedia> normalizedMedia = place.getMediaList() == null
                ? new ArrayList<>()
                : new ArrayList<>(place.getMediaList());

        for (PlaceMedia media : normalizedMedia) {
            media.setPlace(place);
        }

        appendTikTokMedia(normalizedMedia, place);
        return normalizedMedia;
    }

    public List<PlaceMedia> buildDetailsMedia(Place place) {
        List<PlaceMedia> mediaItems = getByPlaceId(place.getId());
        appendTikTokMedia(mediaItems, place);
        return mediaItems.stream()
                .sorted(Comparator
                        .comparing((PlaceMedia media) -> "tiktok".equalsIgnoreCase(media.getSourcePlatform()) ? 0 : 1)
                        .thenComparing(PlaceMedia::getViewCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(PlaceMedia::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<TikTokMediaPreviewResponse> syncTikTokMedia(Place place, List<TikTokMediaPreviewResponse> previews) {
        if (place == null || place.getId() == null) {
            return List.of();
        }

        List<TikTokMediaPreviewResponse> normalizedPreviews = previews == null
                ? List.of()
                : previews.stream().limit(3).toList();
        List<PlaceMedia> existingMedia = getByPlaceId(place.getId());
        Set<String> previewUrls = normalizedPreviews.stream()
                .map(TikTokMediaPreviewResponse::getVideoUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<PlaceMedia> staleTikTokMedia = existingMedia.stream()
                .filter(media -> "tiktok".equalsIgnoreCase(media.getSourcePlatform()))
                .filter(media -> media.getUrl() != null && !previewUrls.contains(media.getUrl()))
                .toList();
        if (!staleTikTokMedia.isEmpty()) {
            placeMediaRepository.deleteAll(staleTikTokMedia);
        }

        for (TikTokMediaPreviewResponse preview : normalizedPreviews) {
            PlaceMedia media = existingMedia.stream()
                    .filter(existing -> Objects.equals(existing.getUrl(), preview.getVideoUrl()))
                    .findFirst()
                    .orElseGet(PlaceMedia::new);

            media.setPlace(place);
            media.setMediaType("tiktok_video");
            media.setUrl(preview.getVideoUrl());
            media.setThumbnail(preview.getThumbnailUrl());
            media.setCaption(preview.getCaption());
            media.setSourcePlatform("tiktok");
            media.setExternalMediaId(preview.getExternalMediaId());
            media.setCreatorName(preview.getCreatorName());
            media.setCreatorHandle(preview.getCreatorHandle());
            media.setViewCount(preview.getPlayCount());
            media.setLikeCount(preview.getLikeCount());
            placeMediaRepository.save(media);
        }

        return normalizedPreviews;
    }

    private void appendTikTokMedia(List<PlaceMedia> mediaItems, Place place) {
        if (place.getTiktokVideoUrl() == null || place.getTiktokVideoUrl().isBlank()) {
            return;
        }

        boolean alreadyIncluded = mediaItems.stream()
                .anyMatch(media -> place.getTiktokVideoUrl().equals(media.getUrl()));
        if (alreadyIncluded) {
            return;
        }

        mediaItems.add(new PlaceMedia(
                null,
                "tiktok_video",
                place.getTiktokVideoUrl(),
                place.getImageUrl(),
                place.getTrendReason(),
                "tiktok",
                null,
                null,
                null,
                null,
                null,
                place
        ));
    }

    private PlaceMedia toEntity(PlaceMediaRequest request, Place place) {
        PlaceMedia media = new PlaceMedia();
        media.setMediaType(request.getMediaType());
        media.setUrl(request.getUrl());
        media.setThumbnail(request.getThumbnail());
        media.setCaption(request.getCaption());
        media.setSourcePlatform(request.getSourcePlatform());
        media.setExternalMediaId(request.getExternalMediaId());
        media.setCreatorName(request.getCreatorName());
        media.setCreatorHandle(request.getCreatorHandle());
        media.setViewCount(request.getViewCount());
        media.setLikeCount(request.getLikeCount());
        media.setPlace(place);
        return media;
    }
}
