package com.bankingsystem.core.features.kyc.domain.repository;

import com.bankingsystem.core.features.kyc.domain.KycUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface KycUploadRepository extends JpaRepository<KycUpload, UUID> {
    long countByIdInAndUploadedBy(Collection<UUID> ids, UUID uploadedBy);
    List<KycUpload> findByIdIn(Collection<UUID> ids);
    long countByUploadedByAndCreatedAtAfter(UUID uploadedBy, java.time.Instant since);
}
