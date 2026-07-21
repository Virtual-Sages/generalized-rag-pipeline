import "./style.scss";

import fileIcon from "../../assets/icons/file-text.svg";
import profileIcon from "../../assets/icons/user-circle.svg";
import avatarImage from "../../assets/user.jpg";
import AuthService from "../../services/authServices";
import { useNavigate } from "react-router-dom";

const Navbar = () => {
    const navigate = useNavigate();

    const handleProfileClick = () => {
        // console.log("Profile button pressed");
    };

    const handleLogoutClick = (e) => {
        e.preventDefault();
        AuthService.logout(navigate);
    };

    return (
        <header className="navbar">
            <div className="navbar-brand">
                <div className="navbar-logo">
                    <img src={fileIcon} alt="" className="icon" />
                </div>
                <span className="navbar-title">AI Document Chat System</span>
            </div>

            <div className="navbar-user">
                <button type="button" className="user-avatar" aria-label="User menu">
                    <img src={avatarImage} alt="User Avatar" />
                </button>

                <div className="dropdown-menu">
                    <button
                        type="button"
                        className="dropdown-item"
                        onClick={handleProfileClick}
                    >
                        <img src={profileIcon} alt="" className="icon" />
                        <span>Profile</span>
                    </button>

                    <hr className="dropdown-divider" />

                    <button
                        type="button"
                        className="dropdown-item dropdown-item--danger"
                        onClick={handleLogoutClick}
                    >
                        <span className="icon" aria-hidden="true" />
                        <span>Logout</span>
                    </button>
                </div>
            </div>
        </header>
    );
};

export default Navbar;
