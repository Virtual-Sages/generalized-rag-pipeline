import './TypingIndicator.scss';

const TypingIndicator = () => {
  return (
    <div className="message-wrapper ai">
      <div className="message-meta">
        <span className="message-avatar ai">
          <span className="material-symbols-outlined">smart_toy</span>
        </span>
        <span className="message-author">AI Assistant</span>
      </div>
      <div className="typing-indicator">
        <span className="typing-dot" />
        <span className="typing-dot" />
        <span className="typing-dot" />
        <span className="typing-label">AI Document Chat System is thinking...</span>
      </div>
    </div>
  );
};

export default TypingIndicator;
