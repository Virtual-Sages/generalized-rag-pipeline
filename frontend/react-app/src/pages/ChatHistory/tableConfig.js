export const chatHistoryTableConfig = {
    columns: [
        {
            colId: "serial",
            header: "S No",
            cell: "serial",
            width: 90,
            maxWidth: 110
        },
        {
            field: "title",
            header: "Chat Title",
            cell: "link",
            flex: 2,
            cellRendererParams: {
                action: "open"
            }
        },
        {
            field: "created_at",
            header: "Created",
            cell: "dateTime",
            flex: 1
        },
        {
            field: "updated_at",
            header: "Last Updated",
            cell: "dateTime",
            flex: 1,
            initialSort: "desc"     // rows are sorted by the updated_at property
        }
    ],
    features: {
        pagination: true,
        pageSize: 10,
        sortable: false
    }
};