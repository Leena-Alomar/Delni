package org.example.delni.Service;


import lombok.RequiredArgsConstructor;
import org.example.delni.Model.City;
import org.example.delni.Repository.CityRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CityService {

    // connect to database
    private final CityRepository cityRepository;

    /// CRUD

    // get all cities
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    // add a city
    public City addCity(City city) {
        return cityRepository.save(city);
    }

    // update a city
    public City updateCity(City city) {
        return cityRepository.save(city);
    }

    // delete a city
    public void deleteCity(Integer id) {
        cityRepository.deleteById(id);
    }

    /// extra endpoints

    // find city by name
    public City findCityByName(String name) {
        return cityRepository.findCityByName(name);
    }

    // find city by region
    public City findCityByRegion(String region) {
        return cityRepository.findCityByRegion(region);
    }

    // find city by id
    public City findCityById(Integer id) {
        return cityRepository.findCityById(id);
    }

}
