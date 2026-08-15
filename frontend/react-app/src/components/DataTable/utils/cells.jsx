// Date time formatter
const formatDateTime = (iso) => {
    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) {
        return {
            date: "-",
            time: "-" 
        };
    }
    return {
        date: date.toLocaleDateString("en-GB"),
        time: date.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })
    };
};

// Serial number formatter
export const SerialCell = (params) => (
    <span className="dt-serial">
        { String((params.node.rowIndex ?? 0) + 1).padStart(2, "0") }
    </span>
);

// connect action to click
export const LinkCell = (params) => {
    if (params.value == null) return "";
    return (
        <button
            type="button"
            className="dt-link"
            onClick={() =>
                params.context?.onCellAction?.(
                    {
                        action: params.action,
                        row: params.data,
                        field: params.colDef.field
                    }
                )
            }
        >
            {params.value}
        </button>
    );
};

// Date with a light time with it
export const DateTimeCell = (params) => {
    if (params.value == null) {
        return "";
    }
    const { date, time } = formatDateTime(params.value);
    return (
        <span className="dt-datetime">
            { date }
            { time && <span className="dt-datetime__time">{ time }</span> }
        </span>
    );
};

// tags related cell
export const TagCell = (params) => {
    if (params.value == null) {
        return "-";
    }
    return (
        <span className="type-tag">
            { params.value }
        </span>
    );
}

export const MemorySizeCell = (params) => {
    const SIZE_UNITS = ["B", "KB", "MB", "GB", "TB"];

    let size = String(params.value ?? "").trim(), unitIndex = 0;

    if (size === "" || size.startsWith("-")) {  // handled negative case in case of an error because that is not possible
        return "-";
    }

    // existing unit detector
    if (!/^\d+(\.\d+)?$/.test(size)) {
        return size;
    }

    size = Number(size);

    while (size >= 1024 && unitIndex < SIZE_UNITS.length - 1) {
        size /= 1024;
        unitIndex += 1;
    }

    return (
        <span>
            { `${size.toFixed(1)} ${SIZE_UNITS[unitIndex]}` }
        </span>
    );
};

export const FileNameCell = (params) => {
    if (params.value == null) {
        return "-";
    }

    const FILE_TYPE_MODIFIERS = {
        PDF:  "dt-file__icon--pdf",
        XLSX: "dt-file__icon--sheet",
        DOCS: "dt-file__icon--doc",
        TEXT: "dt-file__icon--text"
    };

    const type = String(params?.data?.type ?? "").toUpperCase();
    const modifier = FILE_TYPE_MODIFIERS[type] ?? "";

    return (
        <span className="dt-file">
            <span className={ `dt-file__icon ${modifier}` } />
            <span className="dt-file__name">{ params.value }</span>
        </span>
    );
}

export const DownloadButtonCell = (params) => {
    if (params.value == null || params.value === "") {
        return "-";
    }

    const fileName = params?.data?.fileName ?? "document";

    return (
        <span className="dt-download">
            <button
                type="button"
                className="dt-download__btn"
                title={ `Download ${fileName}` }
                aria-label={ `Download ${fileName}` }
                onClick={() =>
                    params.context?.onCellAction?.(
                        {
                            action: "download",
                            row: params.data,
                            field: params.colDef.field
                        }
                    )
                }
            >
                <span className="dt-download__icon" />
            </button>
        </span>
    );
}