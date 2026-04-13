package org.example.delni.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiResponse;
import org.example.delni.DTO.In.CityRequest;
import org.example.delni.Service.CityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public ResponseEntity<?> getAllCities() {
        return ResponseEntity.status(200).body(cityService.getAllCityResponses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCityById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(cityService.getCityResponseById(id));
    }

    @PostMapping
    public ResponseEntity<?> addCity(@RequestBody @Valid CityRequest request) {
        return ResponseEntity.status(200).body(cityService.addCityResponse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCity(@PathVariable Integer id, @RequestBody @Valid CityRequest request) {
        return ResponseEntity.status(200).body(cityService.updateCityResponse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCity(@PathVariable Integer id) {
        cityService.deleteCity(id);
        return ResponseEntity.status(200).body(new ApiResponse("City deleted"));
    }
}
