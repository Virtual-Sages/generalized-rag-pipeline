import './Auth.scss';

import React, { useState } from "react";
import DocumentIcon from "../../assets/icons/file-text.svg";
import UserIcon from "../../assets/icons/user.svg";
import LockIcon from "../../assets/icons/lock.svg";
import EyeIcon from "../../assets/icons/eye.svg";
import EyeOffIcon from "../../assets/icons/eye-off.svg";
import ArrowForwardIcon from "../../assets/icons/arrow-forward.svg";
import EmailIcon from "../../assets/icons/email.svg";
import Spinner from "../../components/Spinner/Spinner";
import { EMAIL_REGEX } from "../../validations/regex";
import makeHttpRequest from "../../services/httpService";
import { useNavigate } from 'react-router-dom';
import NotificationService from '../../services/notificationService';
import getErrorMessage from '../../utils/errorUtils';

export default function Auth() {
  const navigate = useNavigate();

  const [isLogin, setIsLogin] = useState(true);
  // const [remember, setRemember] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState({
    password: false,
    confirmPassword: false
  });
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    password: "",
    confirmPassword: ""
  });
  const [errors, setErrors] = useState({
    username: "",
    email: "",
    password: "",
    confirmPassword: ""
  })

  /***************************************Functions************************************** */
  const validateField = (name, value) => {
    switch (name) {
      case "username":
        if (!value) return "Username is required";
        return "";

      case "email":
        if (!value) return "Email is required";
        if (!EMAIL_REGEX.test(value)) return "Invalid email";
        return "";

      case "password":
        if (!value) return "Password is required";
        return "";

      case "confirmPassword":
        if (!value) return "Confirm Password is required";
        if (value !== formData.password) return "Passwords do not match";
        return "";

      default:
        return "";
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;

    setFormData((el) => ({
      ...el,
      [name]: value
    }));
  }

  const handleBlur = (e) => {
    const { name, value } = e.target;
    const trimmedValue = value.trim();

    setFormData((prev) => ({
      ...prev,
      [name]: trimmedValue,
    }));

    setErrors((prev) => ({
      ...prev,
      [name]: validateField(name, trimmedValue),
    }));
  };

  const handleAuthModeChange = (e) => {
    e.preventDefault();

    setIsLogin(prev => !prev);
    setIsLoading(false);
    setErrors({
      username: "",
      email: "",
      password: "",
      confirmPassword: ""
    });
    setFormData({
      username: "",
      email: "",
      password: "",
      confirmPassword: ""
    });
    setShowPassword({
      password: false,
      confirmPassword: false
    });
  }

  const handleSubmit = async (e) => {
    e.preventDefault();

    const fieldsToValidate = isLogin
      ? ["username", "password"]
      : ["username", "email", "password", "confirmPassword"];
    const newErrors = {};

    fieldsToValidate.forEach((key) => {
      newErrors[key] = validateField(key, formData[key]);
    });

    setErrors(newErrors);

    if (Object.values(newErrors).some(Boolean)) {
      return;
    }

    const data = {
      username: formData.username.trim(),
      ...(!isLogin && { email: formData.email.trim() }),
      password: formData.password.trim(),
    };

    try {
      setIsLoading(true);
      const response = await makeHttpRequest({
        method: 'POST',
        url: isLogin ? '/auth/login' : '/auth/register',
        data
      });

      console.log(response);

      localStorage.setItem("token", response?.accessToken);

      NotificationService.success(
        isLogin
          ? "Signed in successfully."
          : "Account created successfully."
      );
      navigate("/", { replace: true });
    } catch (error) {
      console.error("Authentication failed:", error);

      const message = await getErrorMessage(error);

      NotificationService.error(message);
    } finally {
      setIsLoading(false);
    }
  }
  /***************************************Functions************************************* */
  return (
    <div className="auth-page">
      <main className="auth-container">
        <div className="auth-card">
          <div className="brand">
            <div className="brand__icon">
              <img src={DocumentIcon} alt="..." />
            </div>
            <div className="brand__text">
              <h1>DocAI Intellect</h1>
              <p>Enterprise Document Intelligence</p>
            </div>
          </div>
          <form className="auth-form" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="username">Username</label>
              <div className="input-wrap">
                <span className="input-icon">
                  <img src={UserIcon} alt='user' />
                </span>
                <input
                  id="username"
                  name="username"
                  type="text"
                  placeholder="username"
                  value={formData.username}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isLoading}
                  required
                />

              </div>
              {errors?.username && (
                <p className="error-text">{errors?.username}</p>
              )}
            </div>
            {!isLogin && <div className="field">
              <label htmlFor="username">Email</label>
              <div className="input-wrap">
                <span className="input-icon">
                  <img src={EmailIcon} alt='email' />
                </span>
                <input
                  id="email"
                  name="email"
                  type="email"
                  placeholder="name@company.com"
                  value={formData.email}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isLoading}
                  required
                />
              </div>
              {errors?.email && (
                <p className="error-text">{errors?.email}</p>
              )}
            </div>}
            <div className="field">
              <div className="field__header">
                <label htmlFor="password">Password</label>
              </div>
              <div className="input-wrap">
                <span className="input-icon">
                  <img src={LockIcon} alt="lock" />
                </span>
                <input
                  id="password"
                  name="password"
                  type={showPassword.password ? "text" : "password"}
                  placeholder="••••••••"
                  value={formData.password}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isLoading}
                  required
                />
                <button
                  type="button"
                  className="toggle-password"
                  onClick={() => setShowPassword((el) => ({
                    ...el,
                    password: !el.password,
                  }))}
                  aria-label={showPassword.password ? "Hide password" : "Show password"}
                >
                  {showPassword.password ? (
                    <img src={EyeIcon} alt="show_password" />
                  ) : (
                    <img src={EyeOffIcon} alt="hide_password" />
                  )}
                </button>
              </div>
              {errors?.password && (
                <p className="error-text">{errors?.password}</p>
              )}
            </div>
            {!isLogin && <div className="field">
              <div className="field__header">
                <label htmlFor="password">Confirm Password</label>
              </div>
              <div className="input-wrap">
                <span className="input-icon">
                  <img src={LockIcon} alt="lock" />
                </span>
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type={showPassword.confirmPassword ? "text" : "password"}
                  placeholder="••••••••"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  onBlur={handleBlur}
                  disabled={isLoading}
                  required
                />
                <button
                  type="button"
                  className="toggle-password"
                  onClick={() => setShowPassword((el) => ({
                    ...el,
                    confirmPassword: !el.confirmPassword,
                  }))}
                  aria-label={showPassword.confirmPassword ? "Hide password" : "Show password"}
                >
                  {showPassword.confirmPassword ? (
                    <img src={EyeIcon} alt="show_password" />
                  ) : (
                    <img src={EyeOffIcon} alt="hide_password" />
                  )}
                </button>
              </div>
              {errors?.confirmPassword && (
                <p className="error-text">{errors?.confirmPassword}</p>
              )}
            </div>}
            {/* {isLogin && <div className="remember">
              <input
                id="remember"
                type="checkbox"
                checked={remember}
                disabled={isLoading}
                onChange={(e) => setRemember(e.target.checked)}
              />
              <label htmlFor="remember">Stay signed in for 30 days</label>
            </div>} */}
            <button
              type="submit"
              className="submit-btn"
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <Spinner size="sm" />
                  Please wait...
                </>
              ) : (
                <>
                  {!isLogin ? "Sign Up" : "Sign In"}
                  <img src={ArrowForwardIcon} alt="go_forward" />
                </>
              )}
            </button>
          </form>

          <div className="divider" />
        </div>

        <div className="footer-links">
          <p>
            New to DocAI Intellect?{" "}
            <button
              type="button"
              className="link link--bold"
              onClick={handleAuthModeChange}
              disabled={isLoading}
            >
              {isLogin ? "Create an account" : "Sign In"}
            </button>
          </p>
        </div>
      </main>
    </div>
  );
}