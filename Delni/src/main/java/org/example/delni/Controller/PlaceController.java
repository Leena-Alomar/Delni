package org.example.delni.Controller;

import org.example.delni.Model.Place;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public class PlaceController {

    @GetMapping("/top")
    public List<Place> getTopPlaces() {
        return placeRepository.findAllByOrderBySmartScoreDesc();
    }
}
