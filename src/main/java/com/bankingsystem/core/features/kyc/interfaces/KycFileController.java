package com.bankingsystem.core.features.kyc.interfaces;

import com.bankingsystem.core.features.kyc.domain.KycUpload;
import com.bankingsystem.core.features.kyc.domain.repository.KycUploadRepository;
import com.bankingsystem.core.modules.common.security.CurrentUserService;
import com.bankingsystem.core.modules.common.support.storage.FileStorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/kyc")
public class KycFileController {

    private final KycUploadRepository uploads;
    private final FileStorageService files;
    private final CurrentUserService currentUser;

    public KycFileController(KycUploadRepository uploads, FileStorageService files, CurrentUserService currentUser) {
        this.uploads = uploads;
        this.files = files;
        this.currentUser = currentUser;
    }

    @GetMapping("/file/{id}")
    public ResponseEntity<ByteArrayResource> get(@PathVariable("id") UUID id, Authentication auth) {
        var who = currentUser.requireUserId(auth);
        KycUpload u = uploads.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND));

        boolean isOwner = who.equals(u.getUploadedBy());
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isOwner && !isAdmin) throw new ResponseStatusException(FORBIDDEN);

        byte[] bytes = files.read(id);
        String ct = Optional.ofNullable(u.getContentType()).orElse("application/octet-stream");
        String name = Optional.ofNullable(u.getOriginalFilename()).orElse(id.toString());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ct))
                .header("Content-Disposition", "inline; filename=\"" + name.replace("\"","") + "\"")
                .body(new ByteArrayResource(bytes));
    }
}
