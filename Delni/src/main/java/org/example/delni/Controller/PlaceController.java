package org.example.delni.Controller;

import jakarta.validation.Valid;
import org.example.delni.API.ApiResponse;
import org.example.delni.DTO.In.PlaceRequest;
import org.example.delni.Service.PlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    public ResponseEntity<?> getAllPlaces(
            @RequestParam(required = false) Integer cityId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String vibeTag,
            @RequestParam(required = false) Boolean trendingOnly) {
        if (cityId != null || category != null || vibeTag != null || trendingOnly != null) {
            return ResponseEntity.status(200).body(placeService.searchPlaceCards(cityId, category, vibeTag, trendingOnly));
        }

        return ResponseEntity.status(200).body(placeService.getAllPlaceCards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPlaceById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(placeService.getPlaceCardById(id));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<?> getPlaceDetails(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(placeService.getPlaceDetails(id));
    }

    @GetMapping("/top")
    public ResponseEntity<?> getTopPlaces() {
        return ResponseEntity.status(200).body(placeService.getTopPlaceCards());
    }

    @GetMapping("/trending")
    public ResponseEntity<?> getTrendingPlaces() {
        return ResponseEntity.status(200).body(placeService.getTrendingPlaceCards());
    }

    @PostMapping
    public ResponseEntity<?> addPlace(@RequestBody @Valid PlaceRequest request) {
        return ResponseEntity.status(200).body(placeService.addPlaceResponse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlace(@PathVariable Integer id, @RequestBody @Valid PlaceRequest request) {
        return ResponseEntity.status(200).body(placeService.updatePlaceResponse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlace(@PathVariable Integer id) {
        placeService.deletePlace(id);
        return ResponseEntity.status(200).body(new ApiResponse("Place deleted"));
    }
}
