const TEST_DELAY = 1000;

// DUMMY DATA
const DUMMY_CHAT_HISTORY = [
    { id: "c1a7f2e0-1111-4a01-9c11-0002", title: "Extract clauses from the vendor MSA",    created_at: "2026-07-27T11:03:00Z", updated_at: "2026-07-30T08:20:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0001", title: "Summarise the Q3 revenue report",        created_at: "2026-07-28T09:14:00Z", updated_at: "2026-07-31T16:42:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0004", title: "Compare the two architecture proposals", created_at: "2026-07-24T10:22:00Z", updated_at: "2026-07-28T17:58:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0003", title: "Onboarding docs — what's missing?",      created_at: "2026-07-26T15:47:00Z", updated_at: "2026-07-29T13:05:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0006", title: "Security policy Q&A",                    created_at: "2026-07-21T14:09:00Z", updated_at: "2026-07-25T11:44:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0005", title: "Draft release notes for v2.1",           created_at: "2026-07-22T08:35:00Z", updated_at: "2026-07-27T09:12:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0008", title: "Explain the ingestion pipeline",         created_at: "2026-07-18T09:00:00Z", updated_at: "2026-07-23T10:16:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0007", title: "Which SLAs mention uptime credits?",     created_at: "2026-07-19T16:51:00Z", updated_at: "2026-07-24T15:30:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0010", title: "Rewrite the API error catalogue",        created_at: "2026-07-15T10:41:00Z", updated_at: "2026-07-20T12:37:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0009", title: "Customer feedback themes, June",         created_at: "2026-07-16T13:27:00Z", updated_at: "2026-07-21T18:03:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0012", title: "Untitled chat",                          created_at: "2026-07-11T12:06:00Z", updated_at: "2026-07-11T12:06:00Z" },
    { id: "c1a7f2e0-1111-4a01-9c11-0011", title: "Find duplicate entries in the index",    created_at: "2026-07-13T17:18:00Z", updated_at: "2026-07-18T14:52:00Z" }
];

// standardize
const toChat = (fetchedChat) => {
    return {
        id: fetchedChat.id,
        title: fetchedChat.title,
        created_at: fetchedChat.created_at,
        updated_at: fetchedChat.updated_at
    }
}

class ChatHistoryService {
    static async getChatHistory() {
        await new Promise((resolve) => setTimeout(resolve, TEST_DELAY));    // mock fetch delay
        return DUMMY_CHAT_HISTORY.map(toChat);
    }
}

export default ChatHistoryService;