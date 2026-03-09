package org.example.delni.Service;


import lombok.RequiredArgsConstructor;
import org.example.delni.Model.Favorite;
import org.example.delni.Repository.FavoriteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    //connect to database
    private final FavoriteRepository favoriteRepository;

    /// CRUD

    // get all favorites
    public List<Favorite> getAllFavorites() {
        return favoriteRepository.findAll();
    }

    // add a favorite
    public Favorite addFavorite(Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    // update a favorite
    public Favorite updateFavorite(Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    // delete a favorite
    public void deleteFavorite(Integer id) {
        favoriteRepository.deleteById(id);
    }

    /// extra endpoints

    // find favorite by id
    public Favorite findFavoriteById(Integer id) {
        return favoriteRepository.findFavoriteById(id);
    }

    // find favorite by user id and place id
    public Favorite findFavoriteByUserIdAndPlaceId(Integer userId, Integer placeId) {
        return favoriteRepository.findFavoriteByUserIdAndPlaceId(userId, placeId);
    }

}
