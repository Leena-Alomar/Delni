package org.example.delni.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiResponse;
import org.example.delni.DTO.In.TripGenerationRequest;
import org.example.delni.DTO.In.TripRequest;
import org.example.delni.Service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<?> getAllTrips() {
        return ResponseEntity.status(200).body(tripService.getAllTripResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTripById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(tripService.getTripResponseById(id));
    }

    @PostMapping
    public ResponseEntity<?> addTrip(@RequestBody @Valid TripRequest request) {
        return ResponseEntity.status(200).body(tripService.addTripResponse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTrip(@PathVariable Integer id, @RequestBody @Valid TripRequest request) {
        return ResponseEntity.status(200).body(tripService.updateTripResponse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrip(@PathVariable Integer id) {
        tripService.deleteTrip(id);
        return ResponseEntity.status(200).body(new ApiResponse("Trip deleted"));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateTrip(@RequestBody @Valid TripGenerationRequest request) {
        return ResponseEntity.status(200).body(tripService.generateTripResponse(request));
    }

    @GetMapping("/{id}/itinerary")
    public ResponseEntity<?> getTripItinerary(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(tripService.getTripItinerary(id));
    }

    @PutMapping("/{tripId}/places/{tripPlaceId}/replace")
    public ResponseEntity<?> replaceTripPlace(@PathVariable Integer tripId, @PathVariable Integer tripPlaceId) {
        return ResponseEntity.status(200).body(tripService.replaceTripPlace(tripId, tripPlaceId));
    }
}
