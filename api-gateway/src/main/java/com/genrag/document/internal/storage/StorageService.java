package com.genrag.document.internal.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Defines operations for storing and retrieving documents.
 */
public interface StorageService {
    /**
     * Stores an uploaded file.
     *
     * @param file the file to store
     * @param extension the file extension, such as ".pdf" or ".txt"
     * @param id the unique identifier used as the stored file name
     */
    public void upload(MultipartFile file, String extension, String id);

    /**
     * Retrieves a stored document.
     *
     * @param id the unique identifier of the document
     * @param extension the file extension of the document
     * @return the stored document as a Resource
     */
    public Resource download(String id, String extension);

    /**
     * Checks whether a document exists in storage.
     *
     * @param id the unique identifier of the document
     * @param extension the file extension of the document
     * @return true if the document exists, otherwise false
     */
    public boolean exists(String id, String extension);

    /**
     * Checks whether the specified file format is supported.
     *
     * @param contentType the MIME type of the file
     * @return true if the format is supported, otherwise false
     */
    public boolean isAllowedFormat(String contentType);

    /**
     * Extracts the extension from a file name.
     *
     * @param fileName the original file name
     * @return the file extension including the dot, such as ".pdf" or ".txt"
     */
    public String getExtension(String fileName);
}