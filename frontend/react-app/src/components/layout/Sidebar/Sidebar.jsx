import React, { useState } from 'react';
import './Sidebar.scss';

import addIcon from '../../../assets/icons/plus.svg';
import chatIcon from '../../../assets/icons/message-square.svg';
import historyIcon from '../../../assets/icons/history.svg';
import profileIcon from '../../../assets/icons/user-circle.svg';
import settingsIcon from '../../../assets/icons/settings.svg';

const Sidebar = () => {
  // Initialize state with 'Chat' as the default active item
  const [activeItem, setActiveItem] = useState('Chat');

  // Function to handle navigation clicks
  const handleNavClick = (e, itemName) => {
    e.preventDefault(); // Prevents the page from jumping to top due to href="#"
    setActiveItem(itemName);
  };

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        <button className="btn-new-analysis">
          <img src={addIcon} alt="Add" className="icon" />
          <span>New Analysis</span>
        </button>

        <a 
          href="#" 
          className={`nav-item ${activeItem === 'Chat' ? 'active' : ''}`}
          onClick={(e) => handleNavClick(e, 'Chat')}
        >
          <img src={chatIcon} alt="Chat" className="icon" />
          <span>Chat</span>
        </a>

        <a 
          href="#" 
          className={`nav-item ${activeItem === 'History' ? 'active' : ''}`}
          onClick={(e) => handleNavClick(e, 'History')}
        >
          <img src={historyIcon} alt="History" className="icon" />
          <span>History</span>
        </a>

        <a 
          href="#" 
          className={`nav-item ${activeItem === 'Profile' ? 'active' : ''}`}
          onClick={(e) => handleNavClick(e, 'Profile')}
        >
          <img src={profileIcon} alt="Profile" className="icon" />
          <span>Profile</span>
        </a>

        <a 
          href="#" 
          className={`nav-item ${activeItem === 'Settings' ? 'active' : ''}`}
          onClick={(e) => handleNavClick(e, 'Settings')}
        >
          <img src={settingsIcon} alt="Settings" className="icon" />
          <span>Settings</span>
        </a>
      </nav>
    </aside>
  );
};

export default Sidebar;