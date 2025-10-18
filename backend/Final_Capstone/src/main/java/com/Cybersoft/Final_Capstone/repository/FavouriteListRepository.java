package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavouriteListRepository extends JpaRepository<Property, Integer> {

    /**
     * Get all favorite properties for a user that are still available
     * @param userId User's ID
     * @return List of available properties in user's favorite list
     */
    @Query("SELECT p FROM UserAccount u JOIN u.favoriteList p WHERE u.id = :userId AND p.status.name = 'AVAILABLE'")
    List<Property> findAvailableFavoritePropertiesByUserId(@Param("userId") Integer userId);

    /**
     * Check if a property is in user's favorite list
     * @param userId User's ID
     * @param propertyId Property's ID
     * @return true if property is in favorite list
     */
    @Query("SELECT COUNT(p) > 0 FROM UserAccount u JOIN u.favoriteList p WHERE u.id = :userId AND p.id = :propertyId")
    boolean existsByUserIdAndPropertyId(@Param("userId") Integer userId, @Param("propertyId") Integer propertyId);
}

