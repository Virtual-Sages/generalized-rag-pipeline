import './Sidebar.scss';

import addIcon from '../../assets/icons/plus.svg';
import chatIcon from '../../assets/icons/message-square.svg';
import historyIcon from '../../assets/icons/history.svg';
import profileIcon from '../../assets/icons/user-circle.svg';
import settingsIcon from '../../assets/icons/settings.svg';
import PAGE_CONSTANTS from '../../constants/pages';

const SIDEBAR_MENU_ITEMS = [
    {
        name: PAGE_CONSTANTS.CHAT,
        label: 'Chat',
        icon: chatIcon
    },
    {
        name: PAGE_CONSTANTS.CHAT_HISTORY,
        label: 'History',
        icon: historyIcon
    },
    {
        name: PAGE_CONSTANTS.PROFILE,
        label: 'Profile',
        icon: profileIcon
    },
    {
        name: PAGE_CONSTANTS.SETTINGS,
        label: 'Settings',
        icon: settingsIcon
    }
];

const Sidebar = ({ currentPage, setCurrentPage }) => {
  // Function to handle navigation clicks
  const handleNavClick = (name) => {
    setCurrentPage(name)
  };

  return (
    <aside className="sidebar">
      <nav className="sidebar-nav">
        <button className="btn-new-analysis">
          <img src={addIcon} alt="Add" className="icon" />
          <span>New Analysis</span>
        </button>

        {
            SIDEBAR_MENU_ITEMS.map((item) => {
                return (
                    <button
                        type='button'
                        key={ item.name }
                        className={ `nav-item ${currentPage === item.name ? 'active' : ''}` }
                        onClick={() => handleNavClick(item.name)}
                    >
                        <img src={ item.icon } alt={ item.label } className="icon" />
                        <span>{ item.label }</span>
                    </button>
                )
            })
        }
      </nav>
    </aside>
  );
};

export default Sidebar;