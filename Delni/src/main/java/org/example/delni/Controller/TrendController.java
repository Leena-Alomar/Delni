package org.example.delni.Controller;

import lombok.RequiredArgsConstructor;
import org.example.delni.DTO.Out.TrendRefreshResponse;
import org.example.delni.Service.TikTokService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TikTokService tikTokService;

    @PostMapping("/update")
    public ResponseEntity<?> updateTrends(@RequestParam(required = false) Integer placeId) {
        if (placeId != null) {
            return ResponseEntity.status(200).body(tikTokService.updateTrendForPlace(placeId));
        }

        return ResponseEntity.status(200).body(tikTokService.updateAllTrends());
    }
}
