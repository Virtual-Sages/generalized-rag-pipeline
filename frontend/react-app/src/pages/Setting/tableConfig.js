/*
    field -> field name matches the data key of field in object that needs to be shown in the column. 
    header -> Header text on top of the column in the table.
    cell -> maps the column to a defined cell type. `cells.jsx` contains all the exisitng cell types and `renderer.js` maps our text to the cell type.
    initialSort -> All the rows are sorted based on the given order for this column.
    flex -> Styling related.
    headerClass -> Styling related.
*/

export const DocumentsTableConfig = {
    columns: [
        {
            field: "fileName",
            header: "Document Name",
            cell: "fileName",
            flex: 3,
        },
        {
            field: "createdAt",
            header: "Created Date Timestamp",
            cell: "dateTime",
            flex: 2,
            initialSort: "desc"     // rows are sorted by the createdAt property
        },
        {
            field: "type",
            header: "Type",
            cell: "tags",
            flex: 1
        },
        {
            field: "size",
            header: "Size",
            cell: "memorySize",
            flex: 1
        },
        {
            field: "downloadLink",
            header: "Download Link",
            cell: "downloadButton",
            headerClass: "ag-right-aligned-header",     // aligning header to right side of the column
            flex: 2
        },
    ],
    features: {
        pagination: true,
        pageSize: 10,
        sortable: false
    }
};