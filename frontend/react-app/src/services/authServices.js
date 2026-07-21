import NotificationService from "./NotificationService";

class AuthService {
    static logout(navigate) {
        localStorage.removeItem("token");
        NotificationService.success("Logged out successfully");
        navigate("/login", { replace: true });
    }
}

export default AuthService;