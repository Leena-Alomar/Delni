package org.example.delni.Controller;

import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiResponse;
import org.example.delni.Service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getFavoritesByUserId(@PathVariable Integer userId) {
        return ResponseEntity.status(200).body(favoriteService.getFavoriteResponsesByUserId(userId));
    }

    @GetMapping("/user/{userId}/places")
    public ResponseEntity<?> getFavoritePlaces(@PathVariable Integer userId) {
        return ResponseEntity.status(200).body(favoriteService.getFavoritePlaces(userId));
    }

    @PostMapping
    public ResponseEntity<?> saveFavorite(@RequestParam Integer userId, @RequestParam Integer placeId) {
        return ResponseEntity.status(200).body(favoriteService.saveFavoriteResponse(userId, placeId));
    }

    @DeleteMapping
    public ResponseEntity<?> removeFavorite(@RequestParam Integer userId, @RequestParam Integer placeId) {
        favoriteService.removeFavorite(userId, placeId);
        return ResponseEntity.status(200).body(new ApiResponse("Favorite removed"));
    }
}
