package com.bankingsystem.core.modules.common.support.storage;

import com.bankingsystem.core.features.kyc.domain.KycUpload;
import com.bankingsystem.core.features.kyc.domain.repository.KycUploadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class FileSystemStorageServiceImpl implements FileStorageService {

    private final Path root;
    private final KycUploadRepository repo;

    public FileSystemStorageServiceImpl(
            @Value("${kyc.storage.root:uploads}") String rootDir,
            KycUploadRepository repo
    ) {
        this.root = Paths.get(rootDir).toAbsolutePath().normalize();
        this.repo = repo;
    }

    @Override
    public StoredUpload store(MultipartFile file, String type, UUID uploadedBy) {
        try {
            String dateDir = LocalDate.now().toString();
            Path dir = root.resolve(dateDir);
            Files.createDirectories(dir);

            String ext = extensionFrom(file);
            UUID id = UUID.randomUUID();
            String stored = id.toString().replace("-", "") + ext;
            Path target = dir.resolve(stored);

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            long total = 0L;

            try (InputStream in = file.getInputStream();
                 OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    sha256.update(buf, 0, n);
                    out.write(buf, 0, n);
                    total += n;
                }
            }

            String hex = HexFormat.of().formatHex(sha256.digest());

            KycUpload u = new KycUpload();
            u.setId(id);
            u.setType(type);
            u.setOriginalFilename(file.getOriginalFilename());
            u.setStoredFilename(stored);
            u.setContentType(file.getContentType());
            u.setSizeBytes(total);
            u.setChecksumSha256(hex);
            u.setStoragePath(target.toString());
            u.setUploadedBy(uploadedBy);

            repo.save(u);
            return new StoredUpload(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    private static String extensionFrom(MultipartFile f) {
        String name = f.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(name);
        if (ext != null && !ext.isBlank()) return "." + ext.toLowerCase();
        String ct = f.getContentType();
        if ("image/jpeg".equalsIgnoreCase(ct)) return ".jpg";
        if ("image/png".equalsIgnoreCase(ct)) return ".png";
        if ("image/webp".equalsIgnoreCase(ct)) return ".webp";
        return ".bin";
    }
}
