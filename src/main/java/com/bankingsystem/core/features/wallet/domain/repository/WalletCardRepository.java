package com.bankingsystem.core.features.wallet.domain.repository;

import com.bankingsystem.core.features.wallet.domain.entity.WalletCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletCardRepository extends JpaRepository<WalletCard, UUID> {

    List<WalletCard> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<WalletCard> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndIsDefaultTrue(UUID userId);

    Optional<WalletCard> findFirstByUserIdAndIsDefaultTrue(UUID userId);

    @Query("select (count(c) > 0) from WalletCard c where c.userId = :userId and c.isDefault = true")
    boolean existsAnyDefaultForUser(@Param("userId") UUID userId);

    @Query("select case when count(c)>0 then true else false end from WalletCard c " +
            "where c.userId = :userId and c.isDefault = true")
    boolean hasDefault(@Param("userId") UUID userId);

}
