package com.Cybersoft.Final_Capstone.repository;

import com.Cybersoft.Final_Capstone.Entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {
    Optional<UserAccount> findByIdAndStatus_Name(int id, String statusName);
    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

//    @Query("SELECT o FROM UserAccount o WHERE o.active = true AND (:keyword IS NULL OR :keyword = '' OR " +
//            "o.fullName LIKE %:keyword% " +
//            "OR o.address LIKE %:keyword% " +
//            "OR o.phoneNumber LIKE %:keyword%) " +
//            "AND LOWER(o.role.name) = 'user'")
//    Page<UserAccount> findAll(@Param("keyword") String keyword, Pageable pageable);
    Optional<UserAccount> findByFacebookAccountId(String facebookAccountId);
    Optional<UserAccount> findByGoogleAccountId(String googleAccountId);
    @Query("SELECT o FROM UserAccount o WHERE o.status.name = 'ACTIVE' AND (" +
            ":keyword IS NULL OR :keyword = '' OR " +
            "LOWER(o.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(o.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "o.phone LIKE CONCAT('%', :keyword, '%')) AND " +
            "LOWER(o.role.name) = 'guest'")
    Page<UserAccount> findAll(@Param("keyword") String keyword, Pageable pageable);
    boolean existsByPhone(String phone);
    Optional<UserAccount> findByPhone(String phone);
}
