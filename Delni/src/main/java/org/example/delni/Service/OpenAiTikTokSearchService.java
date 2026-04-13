package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.DTO.External.OpenAiTikTokResultCandidate;
import org.example.delni.DTO.External.OpenAiTikTokResultReview;
import org.example.delni.DTO.External.OpenAiTikTokSearchPlan;
import org.example.delni.Model.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OpenAiTikTokSearchService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiTikTokSearchService.class);

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.model:gpt-5.4-mini}")
    private String model;

    @Value("${openai.api.url:https://api.openai.com/v1/responses}")
    private String responsesUrl;

    private final RestTemplate restTemplate;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public OpenAiTikTokSearchPlan generateSearchPlan(Place place, List<String> heuristicKeywords) {
        if (!isEnabled()) {
            return fallbackPlan(heuristicKeywords);
        }

        try {
            String response = sendStructuredRequest(
                    "tiktok_search_plan",
                    buildSearchPlanInstructions(),
                    buildSearchPlanInput(place, heuristicKeywords),
                    buildSearchPlanSchema()
            );

            String outputText = extractOutputText(response);
            OpenAiTikTokSearchPlan parsedPlan = parseSearchPlan(outputText);
            return normalizePlan(parsedPlan, heuristicKeywords);
        } catch (Exception exception) {
            log.warn("OpenAI TikTok search plan generation failed: {}", exception.getMessage());
            return fallbackPlan(heuristicKeywords);
        }
    }

    public OpenAiTikTokResultReview reviewCandidates(Place place, List<OpenAiTikTokResultCandidate> candidates) {
        if (!isEnabled() || candidates == null || candidates.isEmpty()) {
            return null;
        }

        try {
            String response = sendStructuredRequest(
                    "tiktok_result_review",
                    buildResultReviewInstructions(),
                    buildResultReviewInput(place, candidates),
                    buildResultReviewSchema()
            );

            String outputText = extractOutputText(response);
            OpenAiTikTokResultReview review = parseResultReview(outputText);
            if (review.getRelevanceScore() == null) {
                review.setRelevanceScore(0.0);
            }
            if (review.getDecision() == null || review.getDecision().isBlank()) {
                review.setDecision("weak_match");
            }
            return review;
        } catch (Exception exception) {
            log.warn("OpenAI TikTok candidate review failed: {}", exception.getMessage());
            return null;
        }
    }

    public String getStrategySummary() {
        if (!isEnabled()) {
            return "Exact name -> name with city -> category and vibe -> English and Arabic discovery keywords.";
        }

        return "OpenAI generates place-aware TikTok keywords first, TikTok search returns candidate videos, "
                + "then OpenAI reviews which keyword best matches the place before scoring it.";
    }

    public String getPromptSummary() {
        if (!isEnabled()) {
            return "Search TikTok like a travel planner: exact place name first, then city, category, vibe, "
                    + "and Arabic/English discovery phrases to find content that can support place ranking and itinerary suggestions.";
        }

        return "OpenAI creates city-aware English and Arabic TikTok search phrases for each place, then reviews the "
                + "returned TikTok candidates to keep only results that are actually relevant to that place.";
    }

    private OpenAiTikTokSearchPlan fallbackPlan(List<String> heuristicKeywords) {
        return new OpenAiTikTokSearchPlan(
                "Fallback heuristic keyword generation",
                heuristicKeywords == null ? List.of() : heuristicKeywords
        );
    }

    private OpenAiTikTokSearchPlan normalizePlan(OpenAiTikTokSearchPlan parsedPlan, List<String> heuristicKeywords) {
        Set<String> keywords = new LinkedHashSet<>();

        if (parsedPlan != null && parsedPlan.getSearchKeywords() != null) {
            for (String keyword : parsedPlan.getSearchKeywords()) {
                addKeyword(keywords, keyword);
            }
        }

        if (heuristicKeywords != null) {
            for (String keyword : heuristicKeywords) {
                addKeyword(keywords, keyword);
            }
        }

        List<String> mergedKeywords = keywords.stream()
                .limit(10)
                .toList();

        String strategy = parsedPlan != null && parsedPlan.getStrategy() != null && !parsedPlan.getStrategy().isBlank()
                ? parsedPlan.getStrategy()
                : "Fallback heuristic keyword generation";

        return new OpenAiTikTokSearchPlan(strategy, mergedKeywords);
    }

    private String sendStructuredRequest(String schemaName, String instructions, String input, String schemaJson) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {
                  "model": "%s",
                  "instructions": "%s",
                  "input": "%s",
                  "max_output_tokens": 400,
                  "text": {
                    "format": {
                      "type": "json_schema",
                      "name": "%s",
                      "strict": true,
                      "schema": %s
                    }
                  }
                }
                """.formatted(
                escapeJson(model),
                escapeJson(instructions),
                escapeJson(input),
                escapeJson(schemaName),
                schemaJson
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                responsesUrl,
                new HttpEntity<>(body, headers),
                String.class
        );

        return response.getBody();
    }

    private String extractOutputText(String response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI response was empty");
        }

        Map<String, Object> parsedResponse = jsonParser.parseMap(response);
        Object outputText = parsedResponse.get("output_text");
        if (outputText instanceof String text && !text.isBlank()) {
            return text;
        }

        StringBuilder builder = new StringBuilder();
        Object output = parsedResponse.get("output");
        if (output instanceof List<?> outputItems) {
            for (Object outputItem : outputItems) {
                if (!(outputItem instanceof Map<?, ?> outputMap)) {
                    continue;
                }

                Object content = outputMap.get("content");
                if (!(content instanceof List<?> contentItems)) {
                    continue;
                }

                for (Object contentItem : contentItems) {
                    if (!(contentItem instanceof Map<?, ?> contentMap)) {
                        continue;
                    }

                    Object type = contentMap.get("type");
                    if (!(type instanceof String typeValue)
                            || (!"output_text".equals(typeValue) && !"text".equals(typeValue))) {
                        continue;
                    }

                    Object textValue = contentMap.get("text");
                    if (textValue instanceof String text) {
                        builder.append(text);
                    } else if (textValue instanceof Map<?, ?> textMap) {
                        Object value = textMap.get("value");
                        if (value instanceof String text) {
                            builder.append(text);
                        }
                    }
                }
            }
        }

        String text = builder.toString().trim();
        if (text.isBlank()) {
            throw new IllegalStateException("OpenAI response did not contain output text");
        }

        return text;
    }

    private String buildSearchPlanInstructions() {
        return "You help a Saudi travel app search TikTok for a specific place. "
                + "Generate short, high-signal TikTok search queries that are most likely to find videos about the exact place, "
                + "or the most relevant city-specific branch when the place is part of a chain. "
                + "Prefer a mix of English and Arabic. Avoid broad generic phrases that would return irrelevant city content. "
                + "Return only the structured JSON object.";
    }

    private String buildSearchPlanInput(Place place, List<String> heuristicKeywords) {
        String cityName = place.getCity() != null ? place.getCity().getName() : "Unknown city";
        String category = place.getCategory() != null ? place.getCategory() : "Unknown category";
        String vibeTag = place.getVibeTag() != null ? place.getVibeTag() : "General";

        return """
                Place profile:
                - Name: %s
                - City: %s
                - Category: %s
                - Vibe: %s
                - Google rating: %s

                Current heuristic keywords:
                %s

                Build 6 to 8 TikTok search keywords ordered from most specific to broader fallback.
                """.formatted(
                place.getName(),
                cityName,
                category,
                vibeTag,
                place.getGoogleRating() != null ? String.format(Locale.US, "%.1f", place.getGoogleRating()) : "unknown",
                heuristicKeywords == null || heuristicKeywords.isEmpty() ? "- none" : "- " + String.join("\n- ", heuristicKeywords)
        );
    }

    private OpenAiTikTokSearchPlan parseSearchPlan(String outputText) {
        Map<String, Object> parsed = jsonParser.parseMap(outputText);
        return new OpenAiTikTokSearchPlan(
                readString(parsed, "strategy"),
                readStringList(parsed.get("searchKeywords"))
        );
    }

    private String buildResultReviewInstructions() {
        return "You review TikTok search candidates for a travel app. "
                + "Choose the keyword whose returned TikTok content is most likely about the requested place in the requested city. "
                + "Exact place-name matches are best. For chain brands, city-level brand content can count when it is still clearly relevant. "
                + "If all results look weak or generic, return decision=no_match and relevanceScore=0.";
    }

    private String buildResultReviewInput(Place place, List<OpenAiTikTokResultCandidate> candidates) {
        String cityName = place.getCity() != null ? place.getCity().getName() : "Unknown city";
        StringBuilder builder = new StringBuilder();
        builder.append("Place profile:\n")
                .append("- Name: ").append(place.getName()).append('\n')
                .append("- City: ").append(cityName).append('\n')
                .append("- Category: ").append(place.getCategory()).append('\n')
                .append("- Vibe: ").append(place.getVibeTag()).append('\n')
                .append("\nCandidate TikTok keyword results:\n");

        for (OpenAiTikTokResultCandidate candidate : candidates) {
            builder.append("- Keyword: ").append(candidate.getKeyword()).append('\n')
                    .append("  Raw score: ").append(candidate.getRawScore()).append('\n')
                    .append("  Matched items: ").append(candidate.getMatchedItems()).append('\n')
                    .append("  Recent videos (last 14 days): ").append(candidate.getRecentVideoCount()).append('\n')
                    .append("  Local Saudi/city signals: ").append(candidate.getLocalVideoCount()).append('\n')
                    .append("  Recent local videos: ").append(candidate.getRecentLocalVideoCount()).append('\n');

            List<String> captions = candidate.getSampleCaptions() == null ? List.of() : candidate.getSampleCaptions();
            if (captions.isEmpty()) {
                builder.append("  Sample captions: none\n");
            } else {
                builder.append("  Sample captions:\n");
                for (String caption : captions) {
                    builder.append("    - ").append(caption).append('\n');
                }
            }
        }

        return builder.toString();
    }

    private OpenAiTikTokResultReview parseResultReview(String outputText) {
        Map<String, Object> parsed = jsonParser.parseMap(outputText);
        return new OpenAiTikTokResultReview(
                readString(parsed, "bestKeyword"),
                readDouble(parsed.get("relevanceScore")),
                readString(parsed, "decision"),
                readString(parsed, "reasoning")
        );
    }

    private String buildSearchPlanSchema() {
        return """
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "strategy": {
                      "type": "string"
                    },
                    "searchKeywords": {
                      "type": "array",
                      "minItems": 4,
                      "maxItems": 8,
                      "items": {
                        "type": "string"
                      }
                    }
                  },
                  "required": ["strategy", "searchKeywords"]
                }
                """;
    }

    private String buildResultReviewSchema() {
        return """
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "bestKeyword": {
                      "type": "string"
                    },
                    "relevanceScore": {
                      "type": "number",
                      "minimum": 0,
                      "maximum": 1
                    },
                    "decision": {
                      "type": "string",
                      "enum": ["strong_match", "partial_match", "weak_match", "no_match"]
                    },
                    "reasoning": {
                      "type": "string"
                    }
                  },
                  "required": ["bestKeyword", "relevanceScore", "decision", "reasoning"]
                }
                """;
    }

    private String readString(Map<String, Object> parsed, String key) {
        Object value = parsed.get(key);
        return value instanceof String text ? text : null;
    }

    private Double readDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<String> readStringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof String text && !text.isBlank()) {
                values.add(text);
            }
        }
        return values;
    }

    private void addKeyword(Set<String> keywords, String keyword) {
        if (keyword == null) {
            return;
        }

        String normalized = keyword.trim().replaceAll("\\s+", " ");
        if (!normalized.isBlank()) {
            keywords.add(normalized);
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
