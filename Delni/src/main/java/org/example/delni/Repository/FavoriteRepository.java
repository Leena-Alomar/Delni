package org.example.delni.Repository;


import org.example.delni.Model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {

    Favorite findFavoriteById(Integer id);

    Favorite findFavoriteByUserIdAndPlaceId(Integer userId, Integer placeId);

}
