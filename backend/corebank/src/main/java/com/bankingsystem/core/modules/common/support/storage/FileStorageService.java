package com.bankingsystem.core.modules.common.support.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileStorageService {

    final class StoredUpload {
        private final UUID id;
        public StoredUpload(UUID id) { this.id = id; }
        public UUID uuid() { return id; }
        public String id() { return id.toString(); }
    }

    StoredUpload store(MultipartFile file, String type, UUID uploadedBy);
}
