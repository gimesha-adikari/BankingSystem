package com.bankingsystem.core.features.kyc.domain.repository;

import com.bankingsystem.core.features.kyc.domain.KycCase;
import com.bankingsystem.core.modules.common.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycCaseRepository extends JpaRepository<KycCase, String> {
    Optional<KycCase> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<KycCase> findByUserIdAndStatus(UUID userId, KycStatus status);
    Page<KycCase> findAllByStatus(KycStatus status, Pageable pageable);
    Optional<KycCase> findFirstByUserIdAndStatusInOrderByCreatedAtDesc(UUID userId, Collection<KycStatus> statuses);
    List<KycCase> findTop50ByStatusOrderByCreatedAtAsc(KycStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
       update KycCase kc
          set kc.processing = true
        where kc.id = :id
          and kc.processing = false
       """)
    int tryMarkProcessing(@Param("id") String id);

    @Query("""
       select kc from KycCase kc
        where kc.processing = false
          and kc.status in :statuses
        order by kc.createdAt asc
       """)
    List<KycCase> findCandidatesForAutoReview(@Param("statuses") List<KycStatus> statuses,
                                              Pageable page);
}
