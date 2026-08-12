import { useState, useEffect, useCallback } from "react";
import DataTable from "../../components/DataTable";
import Spinner from "../../components/Spinner/Spinner";
import DocumentService from "../../services/DocumentService";
import NotificationService from "../../services/notificationService";
import { DocumentsTableConfig } from "./tableConfig";
import uploadIcon from "../../assets/icons/upload-file.svg";
import "./Setting.scss";

const Settings = () => {
    const [uploadedDocuments, setUploadedDocuments] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const controller = new AbortController();

        const loadUploadedDocuments = async () => {
            try {
                const foundDocuments = await DocumentService.getUploadedDocument();
                setUploadedDocuments(foundDocuments);
            } catch {
                NotificationService.error("Error loading uploaded documents");
            } finally {
                setIsLoading(false);
            }
        };

        loadUploadedDocuments();

        return () => controller.abort();    // multi request cleanup, just in case
    }, []);

    const handleCellAction = useCallback(async ({ action, row }) => {
        if (action === "download") {
            try {
                await DocumentService.downloadDocument(row);
            } catch {
                // console.log("An error occured");
            }
        }
    }, []);

    const uploadDocument = () => {
        console.log("Upload document");
    };

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
                        onClick={ uploadDocument }
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
        </section>
    );
}

export default Settings;