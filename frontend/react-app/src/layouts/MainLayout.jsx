import { useState } from 'react'
import Navbar from '../components/Navbar/Navbar'
import Sidebar from '../components/Sidebar/Sidebar'
import Footer from '../components/Footer/Footer'
import ChatPage from '../pages/Chat/ChatPage'
import ChatHistory from '../pages/ChatHistory/ChatHistory'
import PAGE_CONSTANTS from '../constants/pages'
import Settings from '../pages/Setting/Setting'
import './MainLayout.scss'
// import { Outlet } from 'react-router-dom'

const PAGES = {
    [PAGE_CONSTANTS.CHAT]: ChatPage,
    [PAGE_CONSTANTS.CHAT_HISTORY]: ChatHistory,
    [PAGE_CONSTANTS.PROFILE]: ChatPage,
    [PAGE_CONSTANTS.SETTINGS]: Settings
};

function MainLayout() {
    const [page, setPage] = useState(PAGE_CONSTANTS.CHAT);
    const [selectedChat, setSelectedChat] = useState(null);
    const ActivePage = PAGES[page] ?? ChatPage;     // fresh chat page as default incase the PAGES doesn't have that key
    // Chat parks the composer between its scroller and the footer, so the
    // footer's scrim has no scrolling content to fade there. Keyed off the
    // component so it survives PAGES being remapped (PROFILE renders ChatPage).
    const isChat = ActivePage === ChatPage;

    return (
        <>
            <Sidebar
                currentPage={page}
                setCurrentPage={setPage}
            />

            <Navbar />

            <div className={ `app-shell ${isChat ? 'app-shell--chat' : ''}` }>
                <ActivePage
                    selectedChat={selectedChat}
                    setSelectedChat={setSelectedChat}
                    setCurrentPage={setPage}
                />
                {/* <Outlet /> */}

                <Footer />
            </div>
        </>
    );
}

export default MainLayout
