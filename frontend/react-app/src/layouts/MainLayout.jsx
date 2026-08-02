import { useState } from 'react'
import Navbar from '../components/Navbar/Navbar'
import Sidebar from '../components/Sidebar/Sidebar'
import ChatPage from '../pages/Chat/ChatPage'
import ChatHistory from '../pages/ChatHistory/ChatHistory'
import PAGE_CONSTANTS from '../constants/pages'
// import { Outlet } from 'react-router-dom'

const PAGES = {
    [PAGE_CONSTANTS.CHAT]: ChatPage,
    [PAGE_CONSTANTS.CHAT_HISTORY]: ChatHistory,
    [PAGE_CONSTANTS.PROFILE]: ChatPage,
    [PAGE_CONSTANTS.SETTINGS]: ChatPage
};

function MainLayout() {
    const [page, setPage] = useState(PAGE_CONSTANTS.CHAT);
    const [selectedChat, setSelectedChat] = useState(null);
    const ActivePage = PAGES[page] ?? ChatPage;     // fresh chat page as default incase the PAGES doesn't have that key

    return (
        <>
            <Sidebar
                currentPage={page}
                setCurrentPage={setPage}
            />

            <Navbar />

            <ActivePage
                selectedChat={selectedChat}
                setSelectedChat={setSelectedChat}
                setCurrentPage={setPage}
            />
            {/* <Outlet /> */}
        </>
    );
}

export default MainLayout
