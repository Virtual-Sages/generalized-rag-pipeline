const getErrorMessage = async (error) => {
    console.log(error);

    const responseData = error?.response?.data;

    // No response data
    if (responseData == null) {
        return "Something went wrong";
    }

    // Blob response
    if (responseData instanceof Blob) {
        if (responseData.size === 0) {
            return "Something went wrong";
        }

        try {
            const text = (await responseData.text()).trim();

            if (!text) {
                return "Something went wrong";
            }

            const data = JSON.parse(text);

            if (
                data !== null &&
                typeof data === "object" &&
                typeof data?.error === "string" &&
                data?.error?.trim()
            ) {
                return data.error.trim();
            }

            return "Something went wrong";
        } catch {
            return "Something went wrong";
        }
    }

    // String response
    if (typeof responseData === "string") {
        const text = responseData?.trim();

        if (!text) {
            return "Something went wrong";
        }

        try {
            const data = JSON?.parse(text);

            if (
                data !== null &&
                typeof data === "object" &&
                typeof data?.error === "string" &&
                data?.error?.trim()
            ) {
                return data.error.trim();
            }
        } catch {
            // Response is plain text
            return text;
        }

        return "Something went wrong";
    }

    // Object response
    if (typeof responseData === "object") {
        if (
            typeof responseData.error === "string" &&
            responseData.error.trim()
        ) {
            return responseData.error.trim();
        }

        return "Something went wrong";
    }
    return "Something went wrong";
};

export default getErrorMessage;