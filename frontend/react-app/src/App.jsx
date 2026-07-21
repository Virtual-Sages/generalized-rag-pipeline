import './App.css'
<<<<<<< Updated upstream
import Sidebar from './components/Sidebar/Sidebar'
=======
import Navbar from './components/layout/Navbar/Navbar'
import Sidebar from './components/layout/Sidebar/Sidebar'
import MessageInput from './components/MessageInput/MessageInput'
import ChatPage from './pages/Chat/ChatPage'
>>>>>>> Stashed changes

function App() {
  return (
    <>
      <div>
<<<<<<< Updated upstream
       <Sidebar />
=======
        <Navbar />
        <Sidebar />
        <ChatPage />
>>>>>>> Stashed changes
      </div>
    </>
  )
}

export default App
