package com.Cybersoft.Final_Capstone.service;

import com.Cybersoft.Final_Capstone.dto.PropertyDTO;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FavouriteListService {
    /**
     * Get all available favorite properties for a user
     * @param userId User's ID
     * @return List of available properties in user's favorite list
     */
    List<PropertyDTO> getAvailableFavoriteProperties(Integer userId);

    /**
     * Get all available favorite properties for a user (paginated)
     * @param userId User's ID
     * @param pageable Pagination parameters
     * @return PageResponse of available properties in user's favorite list
     */
    PageResponse<PropertyDTO> getAvailableFavoriteProperties(Integer userId, Pageable pageable);

    /**
     * Add a property to user's favorite list
     * @param userId User's ID
     * @param propertyId Property's ID
     */
    void addPropertyToFavorites(Integer userId, Integer propertyId);

    /**
     * Remove a property from user's favorite list
     * @param userId User's ID
     * @param propertyId Property's ID
     */
    void removePropertyFromFavorites(Integer userId, Integer propertyId);

    /**
     * Check if a property is in user's favorite list
     * @param userId User's ID
     * @param propertyId Property's ID
     * @return true if property is in favorite list
     */
    boolean isFavorite(Integer userId, Integer propertyId);
}

