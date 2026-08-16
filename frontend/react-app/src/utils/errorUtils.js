const getErrorMessage = async (error) => {
    const responseData = error?.response?.data;

    if (!responseData) {
        return "Something went wrong";
    }

    if (responseData instanceof Blob) {
        if (responseData.size === 0) {
            return "Something went wrong";
        }

        try {
            const text = await responseData.text();
            const data = JSON.parse(text);

            return data?.error || "Something went wrong";
        } catch {
            return "Something went wrong";
        }
    }

    return responseData?.error || "Something went wrong";
};

export default getErrorMessage;