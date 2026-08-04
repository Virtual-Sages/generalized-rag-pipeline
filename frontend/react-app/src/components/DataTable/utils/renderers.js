import { SerialCell, LinkCell, DateTimeCell } from "./cells";

// This file is keeping the table config file onyl text and not using actual components
export const renderers = {
    serial: SerialCell,
    link: LinkCell,
    dateTime: DateTimeCell
};
