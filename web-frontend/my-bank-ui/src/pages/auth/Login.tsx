// src/pages/auth/Login.tsx
import React, { useState } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import InputField from "../../components/InputField";
import Button from "../../components/Button";
import AuthFormWrapper from "../../components/AuthFormWrapper";
import ForgotPasswordModal from "../../models/ForgotPasswordModal";
import { useAlert } from "@/contexts/use-alert";
import { useAuth } from "@/contexts/auth-context";

type Role = "ADMIN" | "CUSTOMER" | "TELLER" | "MANAGER";

const roleHome: Record<Role, string> = {
    CUSTOMER: "/customer/home",
    ADMIN: "/admin/home",
    TELLER: "/teller/home",
    MANAGER: "/manager/home",
};

const Login: React.FC = () => {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [forgotOpen, setForgotOpen] = useState(false);
    const [forgotEmail, setForgotEmail] = useState("");

    const navigate = useNavigate();
    const location = useLocation();
    const { showAlert } = useAlert();
    const { login } = useAuth();

    const stateFrom = (location.state as any)?.from?.pathname as string | undefined;
    const qsFrom = new URLSearchParams(location.search).get("from") || undefined;
    const fromAfterLogin = stateFrom || qsFrom;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            const res = await fetch("/api/v1/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
            });

            const ct = res.headers.get("content-type") || "";
            const payload = ct.includes("application/json") ? await res.json() : await res.text();

            if (!res.ok) {
                const msg =
                    (typeof payload === "string" ? payload : payload?.error || payload?.message) ||
                    "Login failed";
                showAlert(msg, "error");
                return;
            }

            const data = payload as { token: string; username: string; role: Role };

            await login(data.token, { username: data.username, role: data.role });
            showAlert("Login successful", "success");

            const fallback = roleHome[data.role] ?? "/";
            navigate(fromAfterLogin || fallback, { replace: true });
        } catch (error) {
            console.error("Login error:", error);
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
                    name="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="ex: Gimesha_13"
                    required
                />
                <InputField
                    label="Password"
                    type="password"
                    name="password"
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
                <Link to="/register" className="text-indigo-600 hover:text-indigo-800 font-semibold">
                    Register here
                </Link>
            </p>

            <ForgotPasswordModal
                isOpen={forgotOpen}
                email={forgotEmail}
                onEmailChange={(e) => setForgotEmail(e.target.value)}
                onSubmit={() => {
                    setForgotOpen(false);
                    setForgotEmail("");
                }}
                onClose={() => setForgotOpen(false)}
            />
        </AuthFormWrapper>
    );
};

export default Login;
