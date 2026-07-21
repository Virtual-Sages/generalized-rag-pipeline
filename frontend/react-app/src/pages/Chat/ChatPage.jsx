import React, { useState } from 'react';
import WelcomeScreen from '../../components/WelcomeScreen/WelcomeScreen'
import MessageInput from '../../components/MessageInput/MessageInput';
import './ChatPage.scss';

const ChatPage = () => {
  const [hasStartedChat, setHasStartedChat] = useState(false);
  const [messages, setMessages] = useState([]);

  const handleSendMessage = (newMessageText) => {
    if (!newMessageText.trim()) return;

    // 1. Hide the WelcomeScreen permanently for this session
    if (!hasStartedChat) {
      setHasStartedChat(true);
    }

    setMessages((prevMessages) => [
      ...prevMessages, 
      { id: Date.now(), text: newMessageText, sender: 'user' }
    ]);

    // Optional: Simulate an AI response after a delay
    setTimeout(() => {
      setMessages((prevMessages) => [
        ...prevMessages,
        { id: Date.now() + 1, text: "I am analyzing your request...", sender: 'ai' }
      ]);
    }, 1500);
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
                <div className="message-bubble">
                  {msg.text}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Pass the submit handler down to the input component */}
      <MessageInput onSendMessage={handleSendMessage} />
    </div>
  );
};

export default ChatPage;