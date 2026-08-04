import { themeQuartz } from "ag-grid-community";

// AG Grid 33+ themes the grid from JS, not CSS. These values mirror
// styles/abstracts/_variables.scss so the table matches the rest of the app —
// keep the two in sync.

const ROW_HEIGHT = 56;
const HEADER_HEIGHT = 48;

const baseParams = {
    accentColor: "#004ac6",
    backgroundColor: "#ffffff",
    foregroundColor: "#434655",
    borderColor: "rgba(195, 198, 215, 0.4)",
    headerBackgroundColor: "#f2f4f6",
    headerTextColor: "#434655",
    rowHoverColor: "#f2f4f6",
    fontFamily: "Inter, sans-serif",
    fontSize: 14,
    headerFontSize: 12,
    headerFontWeight: 600,
    rowHeight: ROW_HEIGHT,
    headerHeight: HEADER_HEIGHT,
    cellHorizontalPadding: 24,
    wrapperBorder: false
};

export const createTableTheme = (overrides = {}) => themeQuartz.withParams({ ...baseParams, ...overrides });

export const tableTheme = createTableTheme();
