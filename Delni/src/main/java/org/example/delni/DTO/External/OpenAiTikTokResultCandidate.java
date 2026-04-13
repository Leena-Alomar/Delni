package org.example.delni.DTO.External;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiTikTokResultCandidate {

    private String keyword;
    private Double rawScore;
    private Integer matchedItems;
    private Integer recentVideoCount;
    private Integer localVideoCount;
    private Integer recentLocalVideoCount;
    private List<String> sampleCaptions;
}
