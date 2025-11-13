package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.dto.PropertyDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.mapper.PropertyMapper;
import com.Cybersoft.Final_Capstone.payload.response.PageResponse;
import com.Cybersoft.Final_Capstone.repository.FavouriteListRepository;
import com.Cybersoft.Final_Capstone.repository.PropertyRepository;
import com.Cybersoft.Final_Capstone.repository.UserAccountRepository;
import com.Cybersoft.Final_Capstone.service.FavouriteListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavouriteListServiceImp implements FavouriteListService {

    @Autowired
    private FavouriteListRepository favouriteListRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Override
    public List<PropertyDTO> getAvailableFavoriteProperties(Integer userId) {
        // Check if user exists
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with ID: " + userId));

        // Get available favorite properties
        List<Property> properties = favouriteListRepository.findAvailableFavoritePropertiesByUserId(userId);

        // Convert to DTO
        return properties.stream()
                .map(PropertyMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<PropertyDTO> getAvailableFavoriteProperties(Integer userId, Pageable pageable) {
        // Check if user exists
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with ID: " + userId));

        // Get available favorite properties with pagination
        Page<Property> propertyPage = favouriteListRepository.findAvailableFavoritePropertiesByUserId(userId, pageable);

        // Convert to DTO
        List<PropertyDTO> propertyDTOs = propertyPage.getContent().stream()
                .map(PropertyMapper::toDTO)
                .toList();

        return PageResponse.<PropertyDTO>builder()
                .content(propertyDTOs)
                .currentPage(propertyPage.getNumber())
                .pageSize(propertyPage.getSize())
                .totalElements(propertyPage.getTotalElements())
                .totalPages(propertyPage.getTotalPages())
                .first(propertyPage.isFirst())
                .last(propertyPage.isLast())
                .empty(propertyPage.isEmpty())
                .build();
    }


    @Override
    @Transactional
    public void addPropertyToFavorites(Integer userId, Integer propertyId) {
        // Check if user exists
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with ID: " + userId));

        // Check if property exists
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found with ID: " + propertyId));

        // Check if already in favorites
        if (favouriteListRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            throw new IllegalStateException("Property is already in favorites");
        }

        // Add to favorites
        user.getFavoriteList().add(property);
        userAccountRepository.save(user);
    }

    @Override
    @Transactional
    public void removePropertyFromFavorites(Integer userId, Integer propertyId) {
        // Check if user exists
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException("User not found with ID: " + userId));

        // Check if property exists
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new DataNotFoundException("Property not found with ID: " + propertyId));

        // Check if in favorites
        if (!favouriteListRepository.existsByUserIdAndPropertyId(userId, propertyId)) {
            throw new IllegalStateException("Property is not in favorites");
        }

        // Remove from favorites
        user.getFavoriteList().remove(property);
        userAccountRepository.save(user);
    }

    @Override
    public boolean isFavorite(Integer userId, Integer propertyId) {
        return favouriteListRepository.existsByUserIdAndPropertyId(userId, propertyId);
    }
}

