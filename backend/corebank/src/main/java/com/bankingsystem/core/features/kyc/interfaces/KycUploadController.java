package com.bankingsystem.core.features.kyc.interfaces;

import com.bankingsystem.core.features.kyc.domain.repository.KycUploadRepository;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import com.bankingsystem.core.modules.common.support.storage.FileStorageService;
import com.bankingsystem.core.features.kyc.interfaces.dto.KycUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;

import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@RestController
@RequestMapping("/kyc")
public class KycUploadController {

    private static final Set<String> ALLOWED_TYPES = Set.of("DOC_FRONT", "DOC_BACK", "SELFIE", "ADDRESS_PROOF");

    private final FileStorageService storage;
    private final CurrentUserService currentUser;
    private final KycUploadRepository uploads;

    public KycUploadController(FileStorageService storage, CurrentUserService currentUser, KycUploadRepository uploads) {
        this.storage = storage;
        this.currentUser = currentUser;
        this.uploads = uploads;
    }

    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public KycUploadResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("type") String type,
            Authentication auth
    ) {
        if (auth == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        if (!ALLOWED_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid type");
        }

        long max = 10L * 1024 * 1024;
        if (file.getSize() > max) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Max 10MB");
        }

        String ct = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase();
        if (!(ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("image/webp"))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "jpeg/png/webp only");
        }

        UUID who = currentUser.requireUserId(auth);
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long todayCount = uploads.countByUploadedByAndCreatedAtAfter(who, todayStart);
        if (todayCount >= 20) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Daily upload limit reached");
        }

        var stored = storage.store(file, type, who);
        return new KycUploadResponse(stored.id());
    }
}
