import './App.css'

import { ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import MainLayout from './layouts/MainLayout';
import { RouterProvider } from 'react-router-dom';
import router from './routes/AppRoutes';

function App() {
  return (
    <>
      <RouterProvider router={router} />
      <ToastContainer />
    </>
  )
}

export default App
