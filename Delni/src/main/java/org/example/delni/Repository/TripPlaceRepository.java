package org.example.delni.Repository;

import org.example.delni.Model.TripPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripPlaceRepository  extends JpaRepository<TripPlace, Integer> {

    List<TripPlace> findAllByTripIdOrderByDayNumberAscOrderInDayAsc(Integer tripId);

}
