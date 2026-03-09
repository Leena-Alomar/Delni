package org.example.delni.Repository;


import org.example.delni.Model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CityRepository extends JpaRepository<City, Integer>{

    City findCityById(Integer id);

    City findCityByName(String name);

    City findCityByRegion(String region);

}
