import React from 'react';
import './Sidebar.scss';

import addIcon from '../../assets/icons/plus.svg';
import chatIcon from '../../assets/icons/message-square.svg';
import historyIcon from '../../assets/icons/history.svg';
import profileIcon from '../../assets/icons/user-circle.svg';
import settingsIcon from '../../assets/icons/settings.svg';

const Sidebar = () => {
  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        <button className="btn-new-analysis">
          <img src={addIcon} alt="Add" className="icon" />
          <span>New Analysis</span>
        </button>

        <a href="#" className="nav-item active">
          <img src={chatIcon} alt="Chat" className="icon" />
          <span>Chat</span>
        </a>

        <a href="#" className="nav-item">
          <img src={historyIcon} alt="History" className="icon" />
          <span>History</span>
        </a>

        <a href="#" className="nav-item">
          <img src={profileIcon} alt="Profile" className="icon" />
          <span>Profile</span>
        </a>

        <a href="#" className="nav-item">
          <img src={settingsIcon} alt="Settings" className="icon" />
          <span>Settings</span>
        </a>
      </nav>
    </aside>
  );
};

export default Sidebar;