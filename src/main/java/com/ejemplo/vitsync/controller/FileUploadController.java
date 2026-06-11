package com.ejemplo.vitsync.controller;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Secure file upload endpoints.
 *
 * <p>Hardening over the original (audit findings V09/V10):</p>
 * <ul>
 *   <li>Storage directory is configurable and lives OUTSIDE the repository
 *       and classpath ({@code vitsync.upload.dir}); on Render this should be a
 *       persistent disk or, better, S3/Cloudinary (the disk is ephemeral).</li>
 *   <li>The real MIME type is detected from the file content with Apache Tika,
 *       not trusted from the client-declared extension.</li>
 *   <li>Only an allow-list of image types is accepted for avatars; size is
 *       capped (2&nbsp;MB avatars).</li>
 *   <li>Stored filenames are random UUIDs; the user-supplied name is never
 *       used to build the path (path-traversal prevention).</li>
 * </ul>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;   // 2 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");
    private static final Map<String, String> EXTENSION_BY_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Tika tika = new Tika();

    /** Directory where uploads are stored (outside the repo). */
    @Value("${vitsync.upload.dir}")
    private String uploadDir;

    /**
     * Uploads an avatar image.
     *
     * @param file multipart image (≤ 2 MB, jpeg/png/webp by content)
     * @return 200 with {@code {"url": "/uploads/<uuid>.<ext>"}}; 400 on empty
     *         or oversized file; 415 on disallowed type; 500 on I/O error
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Archivo vacío"));
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("error", "El avatar supera el tamaño máximo (2 MB)"));
        }

        try {
            // MIME real por contenido (magic bytes), no por la extensión del cliente
            String detectedType;
            try (InputStream in = file.getInputStream()) {
                detectedType = tika.detect(in);
            }
            if (!ALLOWED_IMAGE_TYPES.contains(detectedType)) {
                logger.warn("Subida rechazada: tipo MIME no permitido {}", detectedType);
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                        .body(Map.of("error", "Tipo de archivo no permitido"));
            }

            // Nombre aleatorio: nunca usamos el nombre original del usuario
            String uniqueName = UUID.randomUUID() + EXTENSION_BY_TYPE.get(detectedType);

            Path directory = Paths.get(uploadDir);
            Files.createDirectories(directory);
            // resolve + normalize + comprobación de contención: anti path traversal
            Path target = directory.resolve(uniqueName).normalize();
            if (!target.startsWith(directory.normalize())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ruta inválida"));
            }
            file.transferTo(target.toFile());

            return ResponseEntity.ok(Map.of("url", "/uploads/" + uniqueName));
        } catch (IOException e) {
            logger.error("Error guardando avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar archivo en el servidor"));
        }
    }
}
