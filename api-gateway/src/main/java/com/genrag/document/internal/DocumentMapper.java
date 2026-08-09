package com.genrag.document.internal;

import com.genrag.document.api.dto.DocumentItem;

/**
 * Provides mapping functionality between document entities and DTOs.
 */
public class DocumentMapper {
    private DocumentMapper(){}

    /**
     * Converts a {@link DocumentEntity} to a {@link DocumentItem} DTO.
     *
     * @param document the document entity to convert
     * @return the corresponding document DTO
     */
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
