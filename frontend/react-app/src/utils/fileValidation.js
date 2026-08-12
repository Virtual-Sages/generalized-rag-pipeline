import {
  ALLOWED_DOCUMENT_EXTENSIONS,
  ALLOWED_DOCUMENT_TYPES,
  MAX_DOCUMENT_SIZE_BYTES,
} from "../constants/documentUpload";

export const formatFileSize = (bytes) => {
  if (!bytes) return "0 B";

  const units = ["B", "KB", "MB", "GB"];
  const exponent = Math.min(
    Math.floor(Math.log(bytes) / Math.log(1024)),
    units.length - 1
  );
  const value = bytes / 1024 ** exponent;

  return `${exponent === 0 ? value : value.toFixed(1)} ${units[exponent]}`;
};

const getExtension = (fileName) => {
  const lastDot = fileName.lastIndexOf(".");
  return lastDot === -1 ? "" : fileName.slice(lastDot).toLowerCase();
};

export const validateDocument = (file) => {
  if (!file || file.size === 0) {
    return "File is empty.";
  }

  const extension = getExtension(file.name || "");
  const isAllowedExtension = ALLOWED_DOCUMENT_EXTENSIONS.includes(extension);
  const isAllowedType = ALLOWED_DOCUMENT_TYPES.includes(file.type);

  if (!isAllowedExtension || !isAllowedType) {
    return "Only PDF and TXT files are supported.";
  }

  if (file.size > MAX_DOCUMENT_SIZE_BYTES) {
    return "File exceeds the 5 MB limit.";
  }

  return null;
};
