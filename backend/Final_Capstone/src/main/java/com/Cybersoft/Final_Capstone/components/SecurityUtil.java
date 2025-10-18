package com.Cybersoft.Final_Capstone.components;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
    public UserAccount getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null &&
                authentication.getPrincipal() instanceof UserAccount selectedUser) {
            if(!selectedUser.getStatus().getName().equals("ACTIVE")) {
                return null;
            }
            return selectedUser;
        }
        return null;
    }
    
    /**
     * Check if the authenticated user has permission to access data for the specified user ID
     * @param requestedUserId The user ID being requested in the URL path
     * @return true if authorized, false otherwise
     */
    public boolean isAuthorizedToAccessUser(Integer requestedUserId) {
        UserAccount currentUser = getLoggedInUser();
        if (currentUser == null || requestedUserId == null) {
            return false;
        }
        return currentUser.getId().equals(requestedUserId);
    }
}
