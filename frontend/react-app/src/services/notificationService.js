import { toast } from "react-toastify";

class NotificationService {
    static success(message) {
        if (message) toast.success(message);
    }

    static error(message) {
        if (message) toast.error(message);
    }

    static info(message) {
        if (message) toast.info(message);
    }

    static warning(message) {
        if (message) toast.warning(message);
    }
}

export default NotificationService;