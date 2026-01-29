package com.skillstorm.finsight.identity_auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.identity_auth.models.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    public AppUser findByUserId(String userId);

    public AppUser findByEmail(String email);

    public boolean existsByEmail(String email);

    @Query("update AppUser u set u.isActive = false where id = :user_id")
    @Modifying
    @Transactional
    public int deactivateUserById(@Param("user_id") String id);

    @Query("update AppUser u set u.isActive = true where id = :user_id")
    @Modifying
    @Transactional
    public int reactivateUserById(@Param("user_id") String id);
}
