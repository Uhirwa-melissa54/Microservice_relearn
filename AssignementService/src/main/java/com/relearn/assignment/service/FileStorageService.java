package com.relearn.assignment.service;

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
 * Handles physical file storage for student submission uploads.
 *
 * Files are stored at: C:/Users/HP/Downloads/submissions
 * Each file is saved with a unique name: originalName_timestamp.ext
 * The stored filename is saved in the Submission.fileUrl column.
 */
@Service
public class FileStorageService {

    private final Path storageLocation;

    public FileStorageService(
            @Value("${file.upload.submissions-dir}") String uploadDir) {
        this.storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create submissions upload directory at: " + uploadDir, e);
        }
    }

    /**
     * Saves an uploaded file to the submissions storage directory.
     *
     * @param file the uploaded MultipartFile
     * @return the stored filename (saved in the database as Submission.fileUrl)
     */
    public String storeFile(MultipartFile file) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");

        if (originalName.contains("..")) {
            throw new RuntimeException("Invalid filename: " + originalName);
        }

        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension    = originalName.substring(dotIndex);
            originalName = originalName.substring(0, dotIndex);
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
     * @param filename the stored filename (from Submission.fileUrl)
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
     * Called when a submission is updated (old file replaced).
     */
    public void deleteFile(String filename) {
        if (filename == null || filename.isBlank()) return;
        try {
            Path filePath = this.storageLocation.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            System.err.println("Warning: could not delete submission file: " + filename);
        }
    }
}
