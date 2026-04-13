package org.example.delni.DTO.External;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiTikTokResultReview {

    private String bestKeyword;
    private Double relevanceScore;
    private String decision;
    private String reasoning;
}
