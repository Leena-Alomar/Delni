package org.example.delni.Repository;

import org.example.delni.Model.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Integer> {

    Optional<Place> findByName(String name);

    List<Place> findAllByOrderBySmartScoreDesc();
}