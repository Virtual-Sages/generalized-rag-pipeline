import { createBrowserRouter } from "react-router-dom";

import MainLayout from "../layouts/MainLayout";
import ProtectedRoute from "./ProtectedRoute";
import Auth from "../pages/Auth/Auth";
import ChatPage from "../pages/Chat/ChatPage";
// import ChatHistory from "../pages/ChatHistory/ChatHistory";

const router = createBrowserRouter([
    {
        path: "/login",
        element: <Auth />,
    },
    {
        path: "/",
        element: (
            <ProtectedRoute>
                <MainLayout />
            </ProtectedRoute>
        ),
        children: [
            {
                index: true,
                element:<ChatPage />,
            },
            // {
            //     path: "chat-history",
            //     element: <ChatHistory />,
            // },
            //   {
            //     path: "profile",
            //     element: ,
            //   },
            //   {
            //     path: "settings",
            //     element: ,
            //   }
        ],
    },
]);

export default router;