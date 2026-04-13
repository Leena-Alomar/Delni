package org.example.delni.Controller;

import lombok.RequiredArgsConstructor;
import org.example.delni.Service.ExploreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/explore")
@RequiredArgsConstructor
public class ExploreController {

    private final ExploreService exploreService;

    @GetMapping("/home")
    public ResponseEntity<?> getHome(@RequestParam Integer cityId,
                                     @RequestParam(required = false) Integer userId) {
        return ResponseEntity.status(200).body(exploreService.getHome(cityId, userId));
    }
}
