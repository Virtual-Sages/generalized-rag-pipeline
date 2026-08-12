import { SerialCell, LinkCell, DateTimeCell, TagCell, MemorySizeCell, FileNameCell, DownloadButtonCell } from "./cells";

// This file is keeping the table config file onyl text and not using actual components
export const renderers = {
    serial: SerialCell,
    link: LinkCell,
    dateTime: DateTimeCell,
    tags: TagCell,
    memorySize: MemorySizeCell,
    fileName: FileNameCell,
    downloadButton: DownloadButtonCell
};
