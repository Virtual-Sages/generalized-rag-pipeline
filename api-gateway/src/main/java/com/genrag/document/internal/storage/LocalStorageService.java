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

    @Override
    public void upload(MultipartFile file, String extension, String id){
        try {
            System.out.println("========== UPLOAD SERVICE HIT ==========");
            System.out.println("Filename: " + file.getOriginalFilename());
            System.out.println("Content Type: " + file.getContentType());
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

    @Override
    public boolean exists(String id, String extension){
        Path target = storageLocation.resolve(
            id + extension
        );

        return Files.exists(target);
    }

    @Override
    public boolean isAllowedFormat(String contentType){

        return "application/pdf".equalsIgnoreCase(contentType)
            || "text/plain".equalsIgnoreCase(contentType);
    }

    @Override
    public String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot == -1) {
            return "";
        }

        return fileName.substring(lastDot);
    }
}