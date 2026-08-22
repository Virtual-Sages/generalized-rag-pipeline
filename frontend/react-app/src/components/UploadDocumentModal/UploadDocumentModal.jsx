import { useRef, useState } from 'react';
import Modal from '../Modal/Modal';
import DocumentService from '../../services/documentService';
import NotificationService from '../../services/notificationService';
import { validateDocument, formatFileSize } from '../../utils/fileValidation';
import { DOCUMENT_ACCEPT_ATTR } from '../../constants/documentUpload';
import fileIcon from '../../assets/icons/file-text.svg';
import './UploadDocumentModal.scss';
import getErrorMessage from '../../utils/errorUtils';

const initialState = {
  selectedFile: null,
  error: null,
  isUploading: false,
  progress: 0,
};

/**
 * Modal for uploading a single PDF/TXT document (max 5MB) via DocumentService.
 * @param {boolean} isOpen
 * @param {() => void} onClose
 * @param {(document: object) => void} [onUploaded] - Called with the created DocumentItem after a successful upload.
 */
const UploadDocumentModal = ({ isOpen, onClose, onUploaded }) => {
  const [state, setState] = useState(initialState);
  const fileInputRef = useRef(null);
  const [isDragActive, setIsDragActive] = useState(false);

  const handleClose = () => {
    if (state.isUploading) return;
    setState(initialState);
    onClose();
  };

  const handleFileSelected = (file) => {
    if (!file) return;

    const validationError = validateDocument(file);
    if (validationError) console.warn('Document rejected:', file.name, validationError);
    setState({
      selectedFile: validationError ? null : file,
      error: validationError,
      isUploading: false,
      progress: 0,
    });
  };

  const handleInputChange = (e) => {
    const file = e.target.files?.[0];
    handleFileSelected(file);
    e.target.value = '';
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragActive(false);
    const file = e.dataTransfer.files?.[0];
    handleFileSelected(file);
  };

  const handleClearFile = () => {
    setState((prev) => ({ ...prev, selectedFile: null, error: null }));
  };

  const handleUpload = async () => {
    if (!state.selectedFile) return;

    setState((prev) => ({ ...prev, isUploading: true, progress: 0, error: null }));

    try {
      const document = await DocumentService.upload(state.selectedFile, {
        onUploadProgress: (event) => {
          if (!event.total) return;
          const progress = Math.round((event.loaded / event.total) * 100);
          setState((prev) => ({ ...prev, progress }));
        },
      });

      NotificationService.success('Document uploaded successfully.');
      onUploaded?.(document);
      setState(initialState);
      onClose();
    } catch (error) {
      console.log("Document uploading error", error);
      
      const message = await getErrorMessage(error);
      
      setState((prev) => ({
        ...prev,
        isUploading: false,
        error: message,
      }));
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title="Upload document"
      closeOnBackdrop={!state.isUploading}
      dismissible={!state.isUploading}
    >
      <ul className="upload-guidelines">
        <li>Supported formats: PDF, TXT</li>
        <li>Maximum size: 5 MB</li>
        <li>One file at a time</li>
      </ul>

      <input
        ref={fileInputRef}
        type="file"
        accept={DOCUMENT_ACCEPT_ATTR}
        className="upload-file-input"
        onChange={handleInputChange}
      />

      {!state.selectedFile ? (
        <div
          className={`upload-dropzone ${isDragActive ? 'active' : ''}`}
          role="button"
          tabIndex={0}
          onClick={() => fileInputRef.current?.click()}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') fileInputRef.current?.click();
          }}
          onDragOver={(e) => {
            e.preventDefault();
            setIsDragActive(true);
          }}
          onDragLeave={() => setIsDragActive(false)}
          onDrop={handleDrop}
        >
          <span>Click to browae or drag a file here</span>
        </div>
      ) : (
        <div className="upload-selected-file">
          <img src={fileIcon} alt="" className="upload-file-icon" />
          <div className="upload-file-info">
            <span className="upload-file-name">{state.selectedFile.name}</span>
            <span className="upload-file-size">{formatFileSize(state.selectedFile.size)}</span>
          </div>
          {!state.isUploading && (
            <button
              type="button"
              className="upload-file-clear"
              aria-label="Remove file"
              onClick={handleClearFile}
            >
              &times;
            </button>
          )}
        </div>
      )}

      {state.error && <p className="upload-error">{state.error}</p>}

      {state.isUploading && (
        <div className="upload-progress">
          <div className="upload-progress-bar" style={{ width: `${state.progress}%` }} />
        </div>
      )}

      <div className="upload-actions">
        <button
          type="button"
          className="btn-cancel"
          onClick={handleClose}
          disabled={state.isUploading}
        >
          Cancel
        </button>
        <button
          type="button"
          className="btn-upload"
          onClick={handleUpload}
          disabled={!state.selectedFile || state.isUploading}
        >
          {state.isUploading ? 'Uploading...' : 'Upload'}
        </button>
      </div>
    </Modal>
  );
};

export default UploadDocumentModal;
