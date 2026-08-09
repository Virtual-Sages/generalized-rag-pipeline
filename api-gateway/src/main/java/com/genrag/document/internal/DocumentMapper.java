package com.genrag.document.internal;

import com.genrag.document.api.dto.DocumentItem;

public class DocumentMapper {
    private DocumentMapper(){}

    public static DocumentItem toDto(DocumentEntity document){
        return new DocumentItem(
            document.getId(),
            document.getFileName(),
            document.getContentType(),
            document.getSizeBytes(),
            document.getStatus(),
            document.getCreatedAt()
        );
    }
}
