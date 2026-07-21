import React from 'react'
import Navbar from '../components/Navbar/Navbar'
import Sidebar from '../components/Sidebar/Sidebar'
import { Outlet } from 'react-router-dom'

function MainLayout() {
    return (
        <>
            <Sidebar />
            <Navbar />
            <Outlet />
        </>
    )
}

export default MainLayout
