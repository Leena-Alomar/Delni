package org.example.delni.DTO.Out;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleSyncResponse {

    private Integer cityId;
    private String cityName;
    private String type;
    private String query;
    private Integer importedCount;
    private Integer trendingCount;
    private String message;
    private String summaryTitle;
    private String summarySubtitle;
    private String nextAction;
    private List<PlaceCardResponse> places;
}
