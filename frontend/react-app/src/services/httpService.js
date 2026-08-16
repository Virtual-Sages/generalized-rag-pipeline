import axios from "axios";

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

    return res?.data;

};

export default makeHttpRequest;