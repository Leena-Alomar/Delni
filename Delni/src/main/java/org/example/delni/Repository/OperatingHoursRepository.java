package org.example.delni.Repository;


import org.example.delni.Model.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperatingHoursRepository extends JpaRepository<OperatingHours, Integer> {


}
