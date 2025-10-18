package com.Cybersoft.Final_Capstone.service.Imp;

import com.Cybersoft.Final_Capstone.dto.UserAccountDTO;
import com.Cybersoft.Final_Capstone.exception.DataNotFoundException;
import com.Cybersoft.Final_Capstone.mapper.UserAccountMapper;
import com.Cybersoft.Final_Capstone.repository.UserAccountRepository;
import com.Cybersoft.Final_Capstone.service.UserAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserAccountServiceImp implements UserAccountService {
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Override
    public UserAccountDTO getUserById(int id) {
        return userAccountRepository.findByIdAndStatus_Name(id, "ACTIVE")
                .map(UserAccountMapper::toDTO)
                .orElseThrow(() -> new DataNotFoundException("User not found with id: " + id));
    }
}
