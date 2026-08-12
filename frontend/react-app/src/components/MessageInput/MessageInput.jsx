import { useState } from 'react';
import './MessageInput.scss';

import addIcon from '../../assets/icons/plus.svg';
import sendIcon from '../../assets/icons/send.svg';
import UploadDocumentModal from '../UploadDocumentModal/UploadDocumentModal';

/**
 * Chat message composer with a send form and a "+" button that opens the document upload modal.
 * @param {(message: string) => void} onSendMessage
 * @param {(document: object) => void} [onDocumentUploaded] - Called after a document is successfully uploaded.
 */
const MessageInput = ({ onSendMessage, onDocumentUploaded }) => {
  const [message, setMessage] = useState('');
  const [isUploadOpen, setIsUploadOpen] = useState(false);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (message.trim()) {
      onSendMessage(message);
      setMessage('');
    }
  };

  const handleDocumentUploaded = (document) => {
    console.log('Document uploaded:', document);
    onDocumentUploaded?.(document);
  };

  return (
    <div className="message-input-wrapper">
      <form className="message-input-container" onSubmit={handleSubmit}>
        <button
          type="button"
          className="btn-add-attachment"
          aria-label="Upload document"
          title="Upload document"
          onClick={() => setIsUploadOpen(true)}
        >
          <img src={addIcon} alt="" className="icon" />
        </button>

        <input 
          type="text" 
          className="chat-input" 
          placeholder="Message AI Document Chat System..."
          value={message}
          onChange={(e) => setMessage(e.target.value)}
        />
        
        <button 
          type="submit" 
          className="btn-send" 
          disabled={!message.trim()}
        >
          <img src={sendIcon} alt="Send" className="icon" />
        </button>
      </form>
      
      {/* Restored Footer Text */}
      <div className="message-footer">
        <span>AI Document Chat System can make mistakes. Check important information.</span>
      </div>

      <UploadDocumentModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onUploaded={handleDocumentUploaded}
      />
    </div>
  );
};

export default MessageInput;