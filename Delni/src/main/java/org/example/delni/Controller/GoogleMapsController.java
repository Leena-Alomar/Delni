package org.example.delni.Controller;

import org.example.delni.Model.Place;
import org.example.delni.Service.GoogleMapsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/maps")
public class GoogleMapsController {

    @Autowired
    private GoogleMapsService googleMapsService;

    @GetMapping("/sync-places")
    public List<Place> syncPlaces(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam String type) {

        // This will call the API, save to DB, and return the saved objects
        return googleMapsService.syncNearbyPlaces(lat, lng, type);
    }
}