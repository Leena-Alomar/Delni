package org.example.delni.DTO.In;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PlaceMediaRequest {

    @NotEmpty(message = "Media type is required")
    @Pattern(regexp = "tiktok_video|image",
            message = "Media type must be either 'tiktok_video' or 'image'")
    private String mediaType;

    @NotEmpty(message = "URL cannot be empty")
    private String url;

    private String thumbnail;
    private String caption;
    private String sourcePlatform;
    private String externalMediaId;
    private String creatorName;
    private String creatorHandle;
    private Long viewCount;
    private Long likeCount;
}
