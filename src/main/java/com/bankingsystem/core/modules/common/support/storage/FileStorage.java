package com.bankingsystem.core.modules.common.support.storage;

import com.bankingsystem.core.features.customer.domain.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "file_storage")
public class FileStorage {

    @Id
    @GeneratedValue
    @Column(name = "file_id", columnDefinition = "BINARY(16)")
    private UUID fileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "checksum", length = 255)
    private String checksum;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
