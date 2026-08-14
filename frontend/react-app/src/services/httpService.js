import axios from "axios";
import NotificationService from "./NotificationService";

const base_url = import.meta.env.VITE_BASE_URL;

const api = axios.create({
    baseURL: base_url,
    withCredentials: true,
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");

    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
});

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        if (
            error.response?.status === 401 &&
            !originalRequest._retry &&
            !originalRequest.url?.includes("/auth/login") &&
            !originalRequest.url?.includes("/auth/refresh")
        ) {
            originalRequest._retry = true;

            if (!originalRequest) {
                return Promise.reject(error);
            }

            try {
                const res = await api.post("/auth/refresh");
                const newAccessToken = res.data.accessToken;

                localStorage.setItem("token", newAccessToken);

                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                return api(originalRequest);
            } catch (err) {
                localStorage.removeItem("token");
                window.location.href = import.meta.env.VITE_LOGIN_URL;
                return Promise.reject(err);
            }
        }

        return Promise.reject(error);
    }
);

const getErrorFromBlob = async (blob) => {
    if (blob.size === 0) {
        return null;
    }

    try {
        return JSON.parse(await blob.text());
    } catch {
        return null;
    }
};

// TODO: Need to de-couple NotificationService from this service. It should be the responsibility of the caller to handle notifications.
// SRP Violation: This service is doing too much by handling both HTTP requests and notifications.
// It should only handle HTTP requests, and the caller should handle notifications based on the response.
const makeHttpRequest = async ({
    method,
    url,
    data = {},
    params = {},
    headers,
    onUploadProgress,
    signal,
    responseType
}) => {
    try {
        const res = await api({
            method,
            url,
            data,
            params,
            ...(
                responseType &&
                {
                    responseType
                }
            ),      // existance check included
            headers,
            onUploadProgress,
            signal,
        });

        if (res?.data?.message) {
            NotificationService.success(res?.data?.message);
        }

        return res?.data;
    } catch (error) {
        // Currently as the SRP is being violated here due to which we are triggering the Notification of error message in the catch block,
        // which inturn is used by all services. So, we check the `error?.response?.data?.error` for all the requests even for those who surely won't have this path.
        // For example blob request. This will be moved/updated when the SRP violation is fixed.

        // Included blob type check

        let errorMessage = "Something went wrong";
        const responseData = error?.response?.data;
        const errorBody = responseData instanceof Blob ? await getErrorFromBlob(responseData) : responseData;
        const errorFromResponse = errorBody?.error;

        if (typeof errorFromResponse === "string" && errorFromResponse.trim() !== "") {
            errorMessage = errorFromResponse;
        }

        NotificationService.error(errorMessage);
        throw error;
    }
};

export default makeHttpRequest;