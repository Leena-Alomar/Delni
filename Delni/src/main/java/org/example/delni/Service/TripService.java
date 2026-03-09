package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.Model.Trip;
import org.example.delni.Repository.TripRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    // connect to database
    private final TripRepository tripRepository;

    ///  CRUD
    // get all trip
    public List<Trip> getAllTrips() {
        return tripRepository.findAll();
    }
    // add a trip
    public Trip addTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    // update a trip
    public Trip updateTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    // delete a trip
    public  void deleteTrip(Integer id) {
        tripRepository.deleteById(id);
    }

    /// extra endpoints
    // find trip by id
    public Trip findTripById(Integer id) {
        return tripRepository.findTripById(id);
    }

    // find trip by user id
    public Trip findTripByUserId(Integer userId) {
        return tripRepository.findTripByUserId(userId);
    }

    // find trip by city id
    public Trip findTripByCityId(Integer cityId) {
        return tripRepository.findTripByCityId(cityId);
    }

    // find trip by user id and city id
    public Trip findTripByUserIdAndCityId(Integer userId, Integer cityId) {
        return tripRepository.findTripByUserIdAndCityId(userId, cityId);
    }
}
