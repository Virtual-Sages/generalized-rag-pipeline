import makeHttpRequest from "./httpService";

// MIME contentType -> filet type
const MIME_TYPES = {
    "application/pdf": "PDF",
    "text/plain": "TEXT",
    "application/vnd.ms-excel": "XLSX",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": "XLSX",
    "application/msword": "DOCS",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "DOCS"
};

// Might appear redundant now but in future will allow ius to map types like CSV to XLSX
const EXTENSIONS = {
    pdf:  "PDF",
    txt:  "TEXT",
    xls:  "XLSX",
    xlsx: "XLSX",
    doc:  "DOCS",
    docx: "DOCS"
};

const getExtension = (fileName) => {
    const name = String(fileName ?? "");
    const lastDotIndex = name.lastIndexOf(".");

    if (lastDotIndex === -1) {
        return "";
    }

    return name.slice(lastDotIndex + 1).toLowerCase();
};

// MIME -> extension -> .extension part of the document
const standardizeDocumentType = (contentType, fileName) => {
    const mime = String(contentType ?? "").split(";")[0].trim().toLowerCase();
    const extension = getExtension(fileName);

    return MIME_TYPES[mime]
        ??
            EXTENSIONS[extension]
        ??
            (extension ? extension.toUpperCase() : null);
};

const generateDownloadLink = (id) => {
    const documentId = String(id ?? "").trim();

    if (documentId === "") {
        return "";
    }

    return `/documents/${encodeURIComponent(documentId)}/download`;
}

// Creates url to trigger downlload of the blob
const saveBlobAsFile = (fileBlob, fileName) => {
    const objectUrl = URL.createObjectURL(fileBlob);
    const anchor = document.createElement("a");

    anchor.href = objectUrl;
    anchor.download = fileName || "document";

    document.body.appendChild(anchor);

    anchor.click();
    anchor.remove();

    // cleanup
    setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
}

// standardize (isolating backend format from frontend)
const toDocument = (fetchedDocument) => {
    return {
        id: fetchedDocument.id,
        fileName: fetchedDocument.fileName,
        type: standardizeDocumentType(fetchedDocument.contentType, fetchedDocument.fileName),
        size: fetchedDocument.sizeBytes,
        createdAt: fetchedDocument.createdAt,
        downloadLink: generateDownloadLink(fetchedDocument.id)
    };
}

class DocumentService {
    // Fetches uploaded documents from the Backend Server
    static async getUploadedDocument() {

        const fetchedDocuments = await makeHttpRequest({
            method: "GET",
            url: "/documents"
        });

        // fertchedDocuments is an array, map all entries to standard document structure
        return Array.isArray(fetchedDocuments) ? fetchedDocuments.map(toDocument) : [];
    }

    static async upload(file, { onUploadProgress, signal } = {}) {
        const formData = new FormData();
        formData.append("file", file);

        const uploadedDocument = await makeHttpRequest({
            method: "POST",
            url: "/documents",
            data: formData,
            onUploadProgress,
            signal
        });

        return uploadedDocument ? toDocument(uploadedDocument) : null;
    }

    // We can't have a simple redirect here as the download endpoint uses Auth headers, which are not set by redirect.
    // So, we use axios to get the blob and pass that to browser for download.
    static async downloadDocument(uploadedDocument) {
        const downloadLink = uploadedDocument?.downloadLink || generateDownloadLink(uploadedDocument?.id);

        if (downloadLink === "") {
            throw new Error("Document is missing a download link");
        }

        const fileBlob = await makeHttpRequest({
            method: "GET",
            url: downloadLink,
            responseType: "blob"
        });

        saveBlobAsFile(fileBlob, uploadedDocument?.fileName);
    }
}

export default DocumentService;
