import React, { useState } from "react";
import InputField from "../../components/InputField";
import Button from "../../components/Button";
import AuthFormWrapper from "../../components/AuthFormWrapper";
import ForgotPasswordModal from "../../models/ForgotPasswordModal";
import { useNavigate } from "react-router-dom";
import { useAlert } from "../../contexts/AlertContext";
import { useAuth } from "../../contexts/AuthContext";

const Login: React.FC = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [forgotOpen, setForgotOpen] = useState(false);
    const [forgotEmail, setForgotEmail] = useState("");
    const navigate = useNavigate();
    const { showAlert } = useAlert();
    const { login } = useAuth(); // ✅ get login from context

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const response = await fetch("/api/v1/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });

            if (!response.ok) {
                const errorText = await response.text();
                if (errorText === '{"error":"Email not verified. Please verify your email first."}') {
                    showAlert("Email not verified. Please verify your email first.", "error");
                } else if (errorText === '{"error":"Invalid username or password"}') {
                    showAlert("Invalid username or password", "error");
                } else if (errorText === '{"error":"User not found"}') {
                    showAlert("User not found", "error");
                } else {
                    showAlert("Login failed: " + errorText, "error");
                }
                return;
            }

            const data = await response.json();

            login(data.token, data.username, data.role);
            showAlert("Login successful", "success");

            switch (data.role) {
                case "CUSTOMER":
                    navigate("/customer/home");
                    break;
                case "ADMIN":
                    navigate("/admin/home");
                    break;
                case "TELLER":
                    navigate("/teller/home");
                    break;
                case "MANAGER":
                    navigate("/manager/home");
                    break;
                default:
                    navigate("/");
                    break;
            }
        } catch (error) {
            console.error("Login error:", error);
            showAlert("Network error, please try again.", "error");
        }
    };

    const handleForgotSubmit = async () => {
        try {
            const response = await fetch(`/api/v1/auth/forgot-password?email=${encodeURIComponent(forgotEmail)}`, {
                method: "POST",
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Something went wrong");
            }

            const message = await response.text();
            alert(message);
            setForgotOpen(false);
            setForgotEmail("");
        } catch (err) {
            console.error("Forgot password error:", err);
            showAlert("Network error, please try again.", "error");
        }
    };

    return (
        <AuthFormWrapper>
            <h2 className="text-3xl font-semibold text-center text-blue-800 mb-6">Log In</h2>

            <form onSubmit={handleSubmit} className="space-y-6">
                <InputField
                    label="User Name"
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="ex: Gimesha_13"
                    required
                />
                <InputField
                    label="Password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                />

                <div className="flex justify-end">
                    <button
                        type="button"
                        onClick={() => setForgotOpen(true)}
                        className="text-sm text-indigo-600 hover:text-indigo-800 font-medium"
                    >
                        Forgot Password?
                    </button>
                </div>

                <Button type="submit" disabled={!username.trim() || !password.trim()} className="w-full mt-2">
                    Log In
                </Button>
            </form>

            <p className="mt-6 text-center text-sm text-gray-600">
                Don’t have an account?{" "}
                <a href="/register" className="text-indigo-600 hover:text-indigo-800 font-semibold">
                    Register here
                </a>
            </p>

            <ForgotPasswordModal
                isOpen={forgotOpen}
                email={forgotEmail}
                onEmailChange={(e) => setForgotEmail(e.target.value)}
                onSubmit={handleForgotSubmit}
                onClose={() => setForgotOpen(false)}
            />
        </AuthFormWrapper>
    );
};

export default Login;
