package org.example.delni.Repository;

import org.example.delni.Model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Integer> {

    Trip findTripById(Integer id);

     Trip findTripByUserId(Integer userId);

     Trip findTripByCityId(Integer cityId);

     Trip findTripByUserIdAndCityId(Integer userId, Integer cityId);

}
