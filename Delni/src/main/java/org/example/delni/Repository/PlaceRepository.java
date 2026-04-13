package org.example.delni.Repository;

import org.example.delni.Model.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Integer> {

    Optional<Place> findByName(String name);

    Optional<Place> findByNameAndCityId(String name, Integer cityId);

    Optional<Place> findByGooglePlaceIdAndCityId(String googlePlaceId, Integer cityId);

    List<Place> findAllByOrderBySmartScoreDesc();

    List<Place> findAllByCityId(Integer cityId);

    List<Place> findAllByCityIdAndNameIgnoreCase(Integer cityId, String name);

    List<Place> findAllByCityIdOrderBySmartScoreDesc(Integer cityId);

    List<Place> findAllByIsTrendingTrueOrderByTiktokTrendScoreDesc();

    List<Place> findAllByCityIdAndCategoryIgnoreCase(Integer cityId, String category);

    List<Place> findAllByCityIdAndVibeTagIgnoreCase(Integer cityId, String vibeTag);
}
