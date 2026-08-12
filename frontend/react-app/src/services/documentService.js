import makeHttpRequest from "./httpService";

class DocumentService {
  static upload(file, { onUploadProgress, signal } = {}) {
    const formData = new FormData();
    formData.append("file", file);

    return makeHttpRequest({
      method: "POST",
      url: "/documents",
      data: formData,
      onUploadProgress,
      signal,
    });
  }
}

export default DocumentService;
