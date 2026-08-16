class AuthService {
    static logout(navigate) {
        localStorage.removeItem("token");
        navigate("/login", { replace: true });
    }
}

export default AuthService;