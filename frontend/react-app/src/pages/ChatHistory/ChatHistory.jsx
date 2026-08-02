import { useState, useEffect, useCallback } from "react";
// import { useNavigate } from "react-router-dom";

import DataTable from "../../components/DataTable";
import Spinner from "../../components/Spinner/Spinner";
import ChatHistoryService from "../../services/chatHistoryService";
import NotificationService from "../../services/notificationService";
import { chatHistoryTableConfig } from "./tableConfig";
import PAGE_CONSTANTS from "../../constants/pages";
import "./ChatHistory.scss";

const ChatHistory = ({ setSelectedChat, setCurrentPage }) => {
    const [historicChats, setHistoricChats] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    // const navigate = useNavigate();

    useEffect(() => {
        const loadChatHistory = async () => {
            try {
                const chats = await ChatHistoryService.getChatHistory();
                setHistoricChats(chats);
            } catch {
                NotificationService.error("Error loading chat history");
            } finally {
                setIsLoading(false);
            }
        };

        loadChatHistory();
    }, []);

    const handleCellAction = useCallback(({ action, row }) => {
        if (action === "open") {
           setSelectedChat(row.id);
           setCurrentPage(PAGE_CONSTANTS.CHAT);
        }
    }, [setSelectedChat, setCurrentPage]);

    return (
        <section className="chat-history">
            <header className="chat-history__header">
                <h1 className="chat-history__title">Chat History</h1>
                <p className="chat-history__subtitle">
                    Review and revisit your past sessions.
                </p>
            </header>

            <div className="chat-history__card">
                {
                    isLoading ? 
                    (
                        <div className="chat-history__loading">
                            <Spinner size="lg" />
                        </div>
                    ) : (
                        <DataTable
                            config={ chatHistoryTableConfig }
                            dataRows={ historicChats }
                            onCellAction={ handleCellAction }
                        />
                    )
                }
            </div>
        </section>
    );
}

export default ChatHistory;