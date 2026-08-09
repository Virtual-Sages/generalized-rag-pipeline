package com.genrag.document.internal.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    public void upload(MultipartFile file, String extension, String id);
    public Resource download(String id, String extension);
    public boolean exists(String id, String extension);
    public boolean isAllowedFormat(String contentType);
    public String getExtension(String fileName);
}