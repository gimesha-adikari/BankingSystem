package com.bankingsystem.core.repository;

import com.bankingsystem.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmail(String email);

    Optional<User> findByEmailVerificationToken(String token);

    List<User> findByRole_RoleName(String roleName);

    @Query("SELECT u FROM User u WHERE u.role.roleName = :roleName AND " +
            "(LOWER(u.firstName) LIKE %:search% OR LOWER(u.lastName) LIKE %:search% OR LOWER(u.email) LIKE %:search%)")
    List<User> searchByRoleAndNameOrEmail(@Param("roleName") String roleName, @Param("search") String search);

}
