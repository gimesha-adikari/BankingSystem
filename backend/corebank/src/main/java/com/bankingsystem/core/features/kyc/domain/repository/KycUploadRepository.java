package com.bankingsystem.core.features.kyc.domain.repository;

import com.bankingsystem.core.features.kyc.domain.KycUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface KycUploadRepository extends JpaRepository<KycUpload, UUID> {
    long countByIdInAndUploadedBy(Collection<UUID> ids, UUID uploadedBy);
}
