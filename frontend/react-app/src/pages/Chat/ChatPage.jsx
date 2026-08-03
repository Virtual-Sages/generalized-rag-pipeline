import React, { useState } from 'react';
import WelcomeScreen from '../../components/WelcomeScreen/WelcomeScreen'
import MessageInput from '../../components/MessageInput/MessageInput';
import TypingIndicator from '../../components/TypingIndicator/TypingIndicator';
import ChatService from '../../services/chatService';
import './ChatPage.scss';

const formatTime = (timestamp) =>
  new Date(timestamp).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });

const ChatPage = () => {
  const [hasStartedChat, setHasStartedChat] = useState(false);
  const [messages, setMessages] = useState([]);
  const [chatId, setChatId] = useState(null);
  const [isAiTyping, setIsAiTyping] = useState(false);

  const handleSendMessage = async (newMessageText) => {
    if (!newMessageText.trim()) return;

    // 1. Hide the WelcomeScreen permanently for this session
    if (!hasStartedChat) {
      setHasStartedChat(true);
    }

    setMessages((prevMessages) => [
      ...prevMessages,
      { id: Date.now(), text: newMessageText, sender: 'user', timestamp: new Date().toISOString() }
    ]);

    setIsAiTyping(true);

    try {
      const assistantMessage = await ChatService.sendMessage(chatId, newMessageText);

      if (!chatId) {
        setChatId(assistantMessage.chatId);
      }

      setMessages((prevMessages) => [
        ...prevMessages,
        {
          id: assistantMessage.id,
          text: assistantMessage.content,
          sender: 'ai',
          timestamp: assistantMessage.createdAt
        }
      ]);
    } catch {
      // makeHttpRequest's interceptor already shows an error toast; nothing further needed here.
    } finally {
      setIsAiTyping(false);
    }
  };

  return (
    <div className="chat-page">
      <div className="chat-content">
        {!hasStartedChat ? (
          <WelcomeScreen isVisible={!hasStartedChat} />
        ) : (
          <div className="messages-list">
            {messages.map((msg) => (
              <div key={msg.id} className={`message-wrapper ${msg.sender}`}>
                <div className="message-meta">
                  {msg.sender === 'user' ? (
                    <>
                      <span className="message-time">{formatTime(msg.timestamp)}</span>
                      <span className="message-author">You</span>
                      <span className="message-avatar user">
                        <span className="material-symbols-outlined">person</span>
                      </span>
                    </>
                  ) : (
                    <>
                      <span className="message-avatar ai">
                        <span className="material-symbols-outlined">smart_toy</span>
                      </span>
                      <span className="message-author">AI Assistant</span>
                      <span className="message-time">{formatTime(msg.timestamp)}</span>
                    </>
                  )}
                </div>
                <div className="message-bubble">
                  {msg.text}
                </div>
              </div>
            ))}
            {isAiTyping && <TypingIndicator />}
          </div>
        )}
      </div>

      {/* Pass the submit handler down to the input component */}
      <MessageInput onSendMessage={handleSendMessage} />
    </div>
  );
};

export default ChatPage;