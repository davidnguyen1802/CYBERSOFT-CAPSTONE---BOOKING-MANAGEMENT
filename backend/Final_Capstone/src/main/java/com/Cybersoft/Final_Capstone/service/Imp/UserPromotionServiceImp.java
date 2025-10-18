package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.dto.UserPromotionDTO;
import com.Cybersoft.Final_Capstone.mapper.UserPromotionMapper;
import com.Cybersoft.Final_Capstone.repository.UserPromotionRepository;
import com.Cybersoft.Final_Capstone.service.UserPromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserPromotionServiceImp implements UserPromotionService {
    @Autowired
    private UserPromotionRepository userPromotionRepository;

    @Override
    public Integer countActivePromotionsByUserId(int userId) {
        return userPromotionRepository.countByUserAccount_IdAndStatus_Name(userId, "ACTIVE");
    }

    @Override
    public List<UserPromotionDTO> getActivePromotionsByUserId(int userId) {
        return userPromotionRepository.findByUserAccount_IdAndStatus_Name(userId, "ACTIVE")
                .stream().map(UserPromotionMapper::toDTO).toList();
    }
}
