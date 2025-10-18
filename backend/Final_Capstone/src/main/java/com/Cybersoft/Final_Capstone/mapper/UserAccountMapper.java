package com.Cybersoft.Final_Capstone.mapper;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import com.Cybersoft.Final_Capstone.dto.UserAccountDTO;

public class UserAccountMapper {
    public static UserAccountDTO toDTO(UserAccount user) {
        UserAccountDTO dto = new UserAccountDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setAvatar(user.getAvatar());
        dto.setGender(user.getGender());
        dto.setDob(user.getDob());
        dto.setRoleName(user.getRole().getName());
        dto.setStatusName(user.getStatus().getName());
        dto.setCreateDate(user.getCreateDate());
        return dto;
    }
}
