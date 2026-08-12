import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import './Modal.scss';

/**
 * Generic centered dialog rendered via a portal to document.body.
 * @param {boolean} isOpen
 * @param {() => void} onClose - Called on Escape, backdrop click, or the close button.
 * @param {string} title
 * @param {boolean} [closeOnBackdrop=true] - Whether a backdrop click closes the modal.
 * @param {boolean} [dismissible=true] - When false, Escape/backdrop/close button are all disabled (e.g. during an in-progress upload).
 */
const Modal = ({ isOpen, onClose, title, children, closeOnBackdrop = true, dismissible = true }) => {
  const dialogRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return undefined;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialogRef.current?.focus();

    const handleKeyDown = (e) => {
      if (e.key !== 'Escape') return;
      if (!dismissible) {
        console.log('Modal: Escape ignored while not dismissible');
        return;
      }
      onClose();
    };
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose, dismissible]);

  if (!isOpen) return null;

  const handleBackdropClick = () => {
    if (closeOnBackdrop && dismissible) onClose();
  };

  return createPortal(
    <div className="modal-backdrop" onClick={handleBackdropClick}>
      <div
        className="modal-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        tabIndex={-1}
        ref={dialogRef}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h2 className="modal-title" id="modal-title">{title}</h2>
          <button
            type="button"
            className="modal-close"
            aria-label="Close"
            disabled={!dismissible}
            onClick={onClose}
          >
            &times;
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>,
    document.body
  );
};

export default Modal;
