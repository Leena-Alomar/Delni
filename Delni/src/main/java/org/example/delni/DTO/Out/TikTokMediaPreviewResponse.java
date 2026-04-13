package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TikTokMediaPreviewResponse {

    private String externalMediaId;
    private String videoUrl;
    private String thumbnailUrl;
    private String creatorName;
    private String creatorHandle;
    private String caption;
    private Long playCount;
    private Long likeCount;
    private Long createTime;
    private Boolean recent;
}
