import React, { useState } from 'react';
import './MessageInput.scss'; 

import addIcon from '../../assets/icons/plus.svg';
import sendIcon from '../../assets/icons/send.svg';

const MessageInput = ({ onSendMessage }) => {
  const [message, setMessage] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (message.trim()) {
      onSendMessage(message);
      setMessage('');
    }
  };

  return (
    <div className="message-input-wrapper">
      <form className="message-input-container" onSubmit={handleSubmit}>
        <button type="button" className="btn-add-attachment">
          <img src={addIcon} alt="Add attachment" className="icon" />
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
    </div>
  );
};

export default MessageInput;