package org.example.delni.Repository;


import org.example.delni.Model.PlaceMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlaceMediaRepository extends JpaRepository<PlaceMedia, Integer> {

     List<PlaceMedia> findAllByPlaceId(Integer placeId);

}
