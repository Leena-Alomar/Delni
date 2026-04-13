package org.example.delni.Controller;

import lombok.RequiredArgsConstructor;
import org.example.delni.DTO.Out.GoogleSyncResponse;
import org.example.delni.Service.GoogleMapsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class GoogleMapsController {

    private final GoogleMapsService googleMapsService;

    @PostMapping("/sync-places")
    public ResponseEntity<?> syncPlaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String type,
            @RequestParam(required = false) Integer cityId) {
        return ResponseEntity.status(200).body(googleMapsService.syncNearbyPlaces(lat, lng, type, cityId));
    }
}
