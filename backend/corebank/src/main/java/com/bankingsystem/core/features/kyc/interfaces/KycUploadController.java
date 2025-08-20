package com.bankingsystem.core.features.kyc.interfaces;

import com.bankingsystem.core.modules.common.security.CurrentUserService;
import com.bankingsystem.core.modules.common.support.storage.FileStorageService;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/kyc")
public class KycUploadController {

    private final FileStorageService storage;
    private final CurrentUserService currentUser;

    public KycUploadController(FileStorageService storage, CurrentUserService currentUser) {
        this.storage = storage;
        this.currentUser = currentUser;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycUploadResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("type") String type,
            Authentication auth
    ) {
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        UUID who = currentUser.requireUserId(auth);
        var stored = storage.store(file, type, who);
        return new KycUploadResponse(stored.id());
    }
}
