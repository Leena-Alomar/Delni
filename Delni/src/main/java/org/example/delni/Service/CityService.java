package org.example.delni.Service;

import lombok.RequiredArgsConstructor;
import org.example.delni.API.ApiException;
import org.example.delni.DTO.In.CityRequest;
import org.example.delni.DTO.Out.CityResponse;
import org.example.delni.Model.City;
import org.example.delni.Repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityRepository cityRepository;

    @Transactional(readOnly = true)
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CityResponse> getAllCityResponses() {
        return cityRepository.findAll().stream()
                .map(this::toCityResponse)
                .toList();
    }

    @Transactional
    public City addCity(City city) {
        return cityRepository.save(city);
    }

    @Transactional
    public CityResponse addCityResponse(City city) {
        return toCityResponse(addCity(city));
    }

    @Transactional
    public CityResponse addCityResponse(CityRequest request) {
        return addCityResponse(toCity(request));
    }

    @Transactional
    public City updateCity(City city) {
        findCityById(city.getId());
        return cityRepository.save(city);
    }

    @Transactional
    public CityResponse updateCityResponse(City city) {
        return toCityResponse(updateCity(city));
    }

    @Transactional
    public CityResponse updateCityResponse(Integer id, CityRequest request) {
        City city = toCity(request);
        city.setId(id);
        return updateCityResponse(city);
    }

    @Transactional
    public void deleteCity(Integer id) {
        findCityById(id);
        cityRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public City findCityByName(String name) {
        City city = cityRepository.findCityByName(name);
        if (city == null) {
            throw new ApiException("City not found");
        }
        return city;
    }

    @Transactional(readOnly = true)
    public City findCityByRegion(String region) {
        City city = cityRepository.findCityByRegion(region);
        if (city == null) {
            throw new ApiException("City not found");
        }
        return city;
    }

    @Transactional(readOnly = true)
    public City findCityById(Integer id) {
        City city = cityRepository.findCityById(id);
        if (city == null) {
            throw new ApiException("City not found");
        }
        return city;
    }

    @Transactional(readOnly = true)
    public CityResponse getCityResponseById(Integer id) {
        return toCityResponse(findCityById(id));
    }

    private CityResponse toCityResponse(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                city.getRegion(),
                city.getLatitude(),
                city.getLongitude(),
                city.getDescription()
        );
    }

    private City toCity(CityRequest request) {
        City city = new City();
        city.setName(request.getName());
        city.setRegion(request.getRegion());
        city.setLatitude(request.getLatitude());
        city.setLongitude(request.getLongitude());
        city.setDescription(request.getDescription());
        return city;
    }
}
