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
}) => {
    try {
        const res = await api({
            method,
            url,
            data,
            params,
            headers,
            onUploadProgress,
            signal,
        });

        if (res?.data?.message) {
            NotificationService.success(res?.data?.message);
        }

        return res?.data;
    } catch (error) {
        NotificationService.error(
            error?.response?.data?.error || "Something went wrong"
        );
        throw error;
    }
};

export default makeHttpRequest;