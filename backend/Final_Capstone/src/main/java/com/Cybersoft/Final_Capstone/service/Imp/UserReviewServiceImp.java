package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.Entity.Property;
import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.Entity.UserReview;
import com.Cybersoft.Final_Capstone.dto.ReviewDTO;
import com.Cybersoft.Final_Capstone.mapper.ReviewMapper;
import com.Cybersoft.Final_Capstone.payload.request.UserReviewReuquest;
import com.Cybersoft.Final_Capstone.repository.PropertyRepository;
import com.Cybersoft.Final_Capstone.repository.UserAccountRepository;
import com.Cybersoft.Final_Capstone.repository.UserReviewRepository;
import com.Cybersoft.Final_Capstone.service.UserReviewService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class UserReviewServiceImp implements UserReviewService {
    @Autowired
    private UserReviewRepository userReviewRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Override
    @Transactional
    public ReviewDTO createReview(UserReviewReuquest request) {
        // Use retry mechanism for optimistic locking
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return createReviewWithOptimisticLocking(request);
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Failed to create review after " + maxRetries + " attempts due to concurrent updates");
                }
                // Brief pause before retry
                try {
                    Thread.sleep(50 * attempt); // Exponential backoff: 50ms, 100ms, 150ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Review creation interrupted");
                }
            }
        }
        throw new RuntimeException("Unexpected error creating review");
    }

    private ReviewDTO createReviewWithOptimisticLocking(UserReviewReuquest request) {
        UserAccount user = userAccountRepository.findByIdAndStatus_Name(request.getUserId(), "ACTIVE")
                .orElseThrow(() -> new RuntimeException("User not found or inactive"));

        // Use regular findById instead of pessimistic locking for better scalability
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Create and save the review first
        UserReview review = new UserReview();
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUser(user);
        review.setProperty(property);

        UserReview savedReview = userReviewRepository.save(review);

        // Optimized: Use incremental calculation instead of recalculating all reviews
        // Formula: newAverage = (oldAverage * oldCount + newRating) / (oldCount + 1)
        BigDecimal currentRating = property.getOverallRating() != null ? property.getOverallRating() : BigDecimal.ZERO;
        int currentCount = property.getReviewCount() != null ? property.getReviewCount() : 0;

        BigDecimal totalRating = currentRating.multiply(BigDecimal.valueOf(currentCount))
                .add(BigDecimal.valueOf(request.getRating()));
        int newCount = currentCount + 1;

        BigDecimal newAverageRating = totalRating.divide(BigDecimal.valueOf(newCount), 1, RoundingMode.HALF_UP);
        property.setOverallRating(newAverageRating);
        property.setReviewCount(newCount);

        // Save will throw ObjectOptimisticLockingFailureException if version changed
        propertyRepository.save(property);

        return ReviewMapper.toDTO(savedReview);
    }

    @Override
    @Transactional
    public ReviewDTO updateReview(int id, UserReviewReuquest request) {
        // Use retry mechanism for optimistic locking
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return updateReviewWithOptimisticLocking(id, request);
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Failed to update review after " + maxRetries + " attempts due to concurrent updates");
                }
                // Brief pause before retry
                try {
                    Thread.sleep(50 * attempt); // Exponential backoff: 50ms, 100ms, 150ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Review update interrupted");
                }
            }
        }
        throw new RuntimeException("Unexpected error updating review");
    }

    private ReviewDTO updateReviewWithOptimisticLocking(int id, UserReviewReuquest request) {
        UserDetails userDetails =(UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!userDetails.getUsername().equals(request.getUserId().toString())){
            throw new RuntimeException("You can only update your own reviews");
        }
        UserReview existingReview = userReviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        int oldRating = existingReview.getRating();
        existingReview.setRating(request.getRating());
        existingReview.setComment(request.getComment());

        UserReview updatedReview = userReviewRepository.save(existingReview);

        // If rating changed, update property's overall rating
        if (oldRating != request.getRating()) {
            Property property = propertyRepository.findById(existingReview.getProperty().getId())
                    .orElseThrow(() -> new RuntimeException("Property not found"));

            // Optimized: Use incremental calculation instead of recalculating all reviews
            // Formula: newAverage = (oldAverage * count - oldRating + newRating) / count
            BigDecimal currentRating = property.getOverallRating() != null ? property.getOverallRating() : BigDecimal.ZERO;
            int count = property.getReviewCount() != null ? property.getReviewCount() : 0;

            if (count > 0) {
                BigDecimal totalRating = currentRating.multiply(BigDecimal.valueOf(count))
                        .subtract(BigDecimal.valueOf(oldRating))
                        .add(BigDecimal.valueOf(request.getRating()));

                BigDecimal newAverageRating = totalRating.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
                property.setOverallRating(newAverageRating);
                propertyRepository.save(property);
            }
        }

        return ReviewMapper.toDTO(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(int id, Integer userId) {
        // Use retry mechanism for optimistic locking
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                deleteReviewWithOptimisticLocking(id, userId);
                return; // Success
            } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new RuntimeException("Failed to delete review after " + maxRetries + " attempts due to concurrent updates");
                }
                // Brief pause before retry
                try {
                    Thread.sleep(50 * attempt); // Exponential backoff: 50ms, 100ms, 150ms
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Review deletion interrupted");
                }
            }
        }
        throw new RuntimeException("Unexpected error deleting review");
    }

    @Override
    public List<ReviewDTO> getReviewsByUserId(int userId) {
        return userReviewRepository.findByUserIdAndProperty_Status_Name(userId, "AVAILABLE").stream().map(ReviewMapper::toDTO).toList();
    }

    private void deleteReviewWithOptimisticLocking(int id, Integer userId) {
        String currentUserId = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        if(!currentUserId.equals(userId.toString())){
            throw new RuntimeException("You can only delete your own reviews");
        }
        UserReview existingReview = userReviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        int deletedRating = existingReview.getRating();

        Property property = propertyRepository.findById(existingReview.getProperty().getId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Delete the review first
        userReviewRepository.delete(existingReview);

        // Optimized: Use incremental calculation instead of recalculating all reviews
        // Formula: newAverage = (oldAverage * oldCount - deletedRating) / (oldCount - 1)
        BigDecimal currentRating = property.getOverallRating() != null ? property.getOverallRating() : BigDecimal.ZERO;
        int currentCount = property.getReviewCount() != null ? property.getReviewCount() : 0;

        int newCount = currentCount - 1;

        if (newCount > 0) {
            BigDecimal totalRating = currentRating.multiply(BigDecimal.valueOf(currentCount))
                    .subtract(BigDecimal.valueOf(deletedRating));

            BigDecimal newAverageRating = totalRating.divide(BigDecimal.valueOf(newCount), 1, RoundingMode.HALF_UP);
            property.setOverallRating(newAverageRating);
            property.setReviewCount(newCount);
        } else {
            // No reviews left, reset to zero
            property.setOverallRating(BigDecimal.ZERO);
            property.setReviewCount(0);
        }

        propertyRepository.save(property); // Will throw exception if version changed
    }
}
