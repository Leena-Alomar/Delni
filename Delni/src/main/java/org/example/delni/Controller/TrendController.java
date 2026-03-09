package org.example.delni.Controller;

import org.example.delni.Model.Place;
import org.example.delni.Repository.PlaceRepository;
import org.example.delni.Service.TikTokService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trends")
public class TrendController {

    @Autowired
    private TikTokService tikTokService;

    @Autowired
    private PlaceRepository placeRepository;

    @GetMapping("/update")
    public List<Place> updateTrends() {

        tikTokService.updateAllTrends();

        return placeRepository.findAll();
    }
}