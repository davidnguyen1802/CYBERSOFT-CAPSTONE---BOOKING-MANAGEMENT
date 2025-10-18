package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FacilityRepository extends JpaRepository<Facility, Integer> {
    Boolean existsByFacilityName(String name);
}
