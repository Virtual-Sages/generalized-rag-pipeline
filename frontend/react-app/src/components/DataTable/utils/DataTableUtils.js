import { renderers as defaultRenderer } from "./renderers";

//   header -> headerName
//   cell   -> cellRenderer, looked up by name in the renderer registry
//   align  -> cellClass
function toColDef({ header, cell, align, colDef, ...obj }, renderers) {
    const cellRenderer = cell ? renderers[cell] : undefined;

    if (!cellRenderer) {
        console.warn(`Unknown cell type: ${cell}`);
    }

    return {
        ...obj,
        ...(header !== undefined && { headerName: header }),
        ...(cellRenderer && { cellRenderer }),
        ...(align && { cellClass: `dt-cell--${align}` }),
        ...colDef
    };
}

export function buildColumnsDefs(columns = [], renderers) {
    const registry = renderers ? { ...defaultRenderer, ...renderers } : defaultRenderer;       // overwrite if passed

    return {
        columnDefs: columns.map((col) => toColDef(col, registry))
    };
}

export function buildRowsRules(row) {
    return {
        rowClassRules: row?.classRules
    };
}

export function buildProperties(features) {
    return {
        pagination: features?.pagination ?? false,
        paginationPageSize: features?.pageSize,
        paginationPageSizeSelector: features?.pageSizeSelector ?? false,
        defaultColDef: {
            sortable: features?.sortable ?? false,
            resizable: features?.resizable ?? true
        },
        domLayout: "autoHeight"
    };
}

// later in the future we can add data source for backend pagionation here
export function buildDataSource(dataRows) {
    return {
        rowData: dataRows
    };
}