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
