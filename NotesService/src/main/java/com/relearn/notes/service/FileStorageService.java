package com.relearn.notes.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Handles physical file storage for note uploads.
 *
 * Files are stored at: C:/Users/HP/Downloads/notes
 * Each file is saved with a unique name: originalName_timestamp.ext
 * The stored filename is saved in the Note.fileUrl column.
 *
 * To download: call loadFile(filename) which returns a Resource
 * that Spring can stream back to the client.
 */
@Service
public class FileStorageService {

    private final Path storageLocation;

    public FileStorageService(@Value("${file.upload.notes-dir}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            // Create the directory if it doesn't exist
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create notes upload directory at: " + uploadDir, e);
        }
    }

    /**
     * Saves an uploaded file to the notes storage directory.
     *
     * The file is renamed to: originalName_timestamp.extension
     * This prevents name collisions when multiple teachers upload
     * files with the same name.
     *
     * @param file the uploaded MultipartFile
     * @return the stored filename (saved in the database as fileUrl)
     */
    public String storeFile(MultipartFile file) {
        // Clean the original filename to prevent path traversal attacks
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");

        // Reject filenames with path traversal sequences
        if (originalName.contains("..")) {
            throw new RuntimeException("Invalid filename: " + originalName);
        }

        // Build a unique filename: name_timestamp.ext
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);           // e.g. ".pdf"
            originalName = originalName.substring(0, dotIndex);     // e.g. "math-ch3"
        }
        String storedFilename = originalName + "_" + System.currentTimeMillis() + extension;

        try {
            Path targetPath = this.storageLocation.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storedFilename, e);
        }
    }

    /**
     * Loads a stored file as a Spring Resource for download/streaming.
     *
     * @param filename the stored filename (from Note.fileUrl)
     * @return a Resource pointing to the file
     * @throws RuntimeException if the file doesn't exist or can't be read
     */
    public Resource loadFile(String filename) {
        try {
            Path filePath = this.storageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or not readable: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path: " + filename, e);
        }
    }

    /**
     * Deletes a stored file from disk.
     * Called when a note is deleted.
     *
     * @param filename the stored filename to delete
     */
    public void deleteFile(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path filePath = this.storageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't throw — note deletion should still succeed
            // even if the physical file is already gone
            System.err.println("Warning: could not delete file: " + filename);
        }
    }

    /**
     * Returns the absolute path of the storage directory.
     * Useful for logging/debugging.
     */
    public String getStorageLocation() {
        return storageLocation.toString();
    }
}
