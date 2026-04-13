package org.example.delni.DTO.External;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiTikTokSearchPlan {

    private String strategy;
    private List<String> searchKeywords;
}
