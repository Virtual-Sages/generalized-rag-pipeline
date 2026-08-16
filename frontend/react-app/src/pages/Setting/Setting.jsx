import { useState, useEffect, useCallback } from "react";
import DataTable from "../../components/DataTable";
import Spinner from "../../components/Spinner/Spinner";
import DocumentService from "../../services/documentService";
import NotificationService from "../../services/notificationService";
import UploadDocumentModal from "../../components/UploadDocumentModal/UploadDocumentModal";
import { DocumentsTableConfig } from "./tableConfig";
import uploadIcon from "../../assets/icons/upload-file.svg";
import getErrorMessage from "../../utils/errorUtils";
import "./Setting.scss";

const Settings = () => {
    const [uploadedDocuments, setUploadedDocuments] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isUploadOpen, setIsUploadOpen] = useState(false);

    const fetchUploadedDocuments = useCallback(async () => {
        try {
            const foundDocuments = await DocumentService.getUploadedDocument();
            setUploadedDocuments(foundDocuments);
        } catch(error) {
            console.log("Document fetching error", error);

            const message = await getErrorMessage(error);

            NotificationService.error(message);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        const fetchOnMount = async () => {
            await fetchUploadedDocuments();
        };

        fetchOnMount();
    }, [ fetchUploadedDocuments ]);

    const handleCellAction = useCallback(async ({ action, row }) => {
        if (action === "download") {
            try {
                await DocumentService.downloadDocument(row);
            } catch(error) {
                console.error("Document download failed:", error);
                
                const message = await getErrorMessage(error);
        
                NotificationService.error(message);
            }
        }
    }, []);

    const handleDocumentUploaded = useCallback(() => {
        fetchUploadedDocuments();
    }, [ fetchUploadedDocuments ]);

    return (
        <section className="chat-history">
            <header className="chat-history__header">
                <h1 className="chat-history__title">Document Management</h1>
                <p className="chat-history__subtitle">
                    Manage you uploaded data and source files.
                </p>
            </header>

            <div className="chat-history__card">
                <div className="settings__card-header">
                    <div className="settings__card-heading">
                        <h3 className="settings__card-title">Recent Uploads</h3>
                        <span className="settings__count">
                            { uploadedDocuments.length } Documents
                        </span>
                    </div>

                    <button
                        type="button"
                        className="settings__upload-btn"
                        onClick={ () => setIsUploadOpen(true) }
                    >
                        <img
                            src={ uploadIcon }
                            alt=""
                            className="settings__upload-icon"
                        />
                        Upload File
                    </button>
                </div>

                {
                    isLoading ? 
                    (
                        <div className="chat-history__loading">
                            <Spinner size="lg" />
                        </div>
                    ) : (
                        <DataTable
                            config={ DocumentsTableConfig }
                            dataRows={ uploadedDocuments }
                            onCellAction={ handleCellAction }
                        />
                    )
                }
            </div>

            <UploadDocumentModal
                isOpen={ isUploadOpen }
                onClose={ () => setIsUploadOpen(false) }
                onUploaded={ handleDocumentUploaded }
            />
        </section>
    );
}

export default Settings;