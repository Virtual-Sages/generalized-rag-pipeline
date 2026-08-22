package com.genrag.document.internal.storage;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.UrlResource;
import java.net.MalformedURLException;

public class LocalStorageService implements StorageService{
    private final Path storageLocation;

    public LocalStorageService(String storageLocation){
        this.storageLocation = Paths.get(storageLocation);
    }

    /**
     * Stores an uploaded file in the configured storage location.
     *
     * @param file the file to be stored
     * @param extension the file extension, such as ".pdf" or ".txt"
     * @param id the unique identifier used as the file name
     * @throws RuntimeException if the file cannot be stored
     */
    @Override
    public void upload(MultipartFile file, String extension, String id){
        try {
            // Create the complete target path
            Path target = storageLocation.resolve(
                id + extension
            );

            // Create the parent directory if it doesn't exist
            Files.createDirectories(target.getParent());

            // Read the uploaded file and store the complete file on disk
            try (var in = file.getInputStream()) {

                Files.copy(
                        in,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return;
        } catch (IOException e) {
             throw new RuntimeException(
                "Failed to store file",
                e
            );
        }
    }

    /**
     * Retrieves a stored file as a Spring Resource.
     *
     * @param id the unique identifier of the document
     * @param extension the file extension, such as ".pdf" or ".txt"
     * @return the stored file as a Resource
     * @throws RuntimeException if the file does not exist, is not readable,
     *                          or the file path is invalid
     */
    @Override
    public Resource download(String id, String extension){
        try{
            Path target = storageLocation.resolve(
                id + extension
            );

            Resource resource = new UrlResource(
                target.toUri()
            );

            if(!resource.exists() || !resource.isReadable()){
                throw new RuntimeException(
                    "File not found" + id
                );
            }

            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid id: " + id, e);
        }
    }

    /**
     * Checks whether a stored document exists in the configured storage location.
     *
     * @param id the unique identifier of the document
     * @param extension the file extension of the document
     * @return true if the file exists, otherwise false
     */
    @Override
    public boolean exists(String id, String extension){
        Path target = storageLocation.resolve(
            id + extension
        );

        return Files.exists(target);
    }

    /**
     * Checks whether the specified content type is supported for document storage.
     *
     * The supported formats are PDF and plain text.
     *
     * @param contentType the MIME type of the uploaded file
     * @return true if the content type is PDF or plain text, otherwise false
     */
    @Override
    public boolean isAllowedFormat(String contentType){

        return "application/pdf".equalsIgnoreCase(contentType)
            || "text/plain".equalsIgnoreCase(contentType);
    }

    /**
     * Extracts the file extension from the specified file name.
     *
     * @param fileName the original file name
     * @return the file extension including the dot, such as ".pdf" or ".txt",
     *         or an empty string if no extension is present
     */
    @Override
    public String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot == -1) {
            return "";
        }

        return fileName.substring(lastDot);
    }

    /**
     * Returns the configured storage location, starting from the "uploads" folder.
     *
     * The absolute prefix is stripped so the value does not depend on where the
     * repository is cloned or which machine the application runs on.
     *
     * @return the storage path relative to the "uploads" folder, such as
     *         "uploads/documents"
     * @throws IllegalStateException if the configured storage location does not
     *                               contain an "uploads" folder
     */
    @Override
    public String getStoragePath(){
        String path = storageLocation.toString().replace('\\', '/');
        int index = path.indexOf("/uploads");

        if(index < 0){
            throw new IllegalStateException("storage.location must contain an 'uploads' folder, but was: " + path); 
        }

        return path.substring(index + 1);
    }
}