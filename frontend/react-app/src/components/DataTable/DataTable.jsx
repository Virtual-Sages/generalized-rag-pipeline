import { useMemo } from "react";
import { AllCommunityModule } from "ag-grid-community";
import { AgGridProvider, AgGridReact } from "ag-grid-react";
import {
    buildColumnsDefs,
    buildRowsRules,
    buildProperties,
    buildDataSource
} from "./utils/DataTableUtils";
import { tableTheme } from "./utils/tableTheme";
import "./styles.scss";

const DataTable = ({ config = {}, dataRows = [], onCellAction }) => {
    const { columns, row, features, customOptions, theme = tableTheme } = config;

    const columnOptions = useMemo(() => buildColumnsDefs(columns), [columns]);
    const rowOptions = useMemo(() => buildRowsRules(row), [row]);
    const propertyOptions = useMemo(() => buildProperties(features), [features]);

    const context = useMemo(() => (
        { onCellAction }
    ), [onCellAction]);

    const options = {
        ...columnOptions,
        ...rowOptions,
        ...propertyOptions,
        ...buildDataSource(dataRows)
    };

    return (
        <AgGridProvider modules={[ AllCommunityModule ]}>
            <div className="dt-grid">
                <AgGridReact
                    theme={ theme }                 // styling
                    context={ context }             // reaches cell renderers
                    { ...options }                  // row and column options  row data + other basic properties
                    { ...customOptions }            // Incase a specific property is there which options don't handle
                />
            </div>
        </AgGridProvider>
    );
}

export default DataTable;