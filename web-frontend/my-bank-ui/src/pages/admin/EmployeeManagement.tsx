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
                    (typeof payload === "string" ? payload : payload?.error || payload?.message) || "Login failed";
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
            <div className="mx-auto flex flex-col items-center gap-2 text-center motion-safe:animate-[fadeIn_.25s_ease-out]">
                <div className="h-11 w-11 rounded-2xl bg-indigo-600/15 ring-1 ring-indigo-500/30 grid place-items-center">
                    <span className="text-xl">🏦</span>
                </div>
                <h2 className="text-2xl md:text-3xl font-semibold tracking-tight text-slate-900">
                    Log In
                </h2>
                <p className="text-sm text-slate-600">Access your MyBank Admin and customer tools.</p>
            </div>

            <form onSubmit={handleSubmit} className="mt-6 space-y-5 motion-safe:animate-[riseIn_.28s_ease-out]">
                <InputField
                    label="User Name"
                    type="text"
                    name="username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    placeholder="ex: Gimesha_13"
                    required
                    tone="light"
                />
                <InputField
                    label="Password"
                    type="password"
                    name="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                    tone="light"
                />

                <div className="flex justify-end">
                    <button
                        type="button"
                        onClick={() => setForgotOpen(true)}
                        className="text-sm font-medium text-indigo-600 hover:text-indigo-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded"
                    >
                        Forgot Password?
                    </button>
                </div>

                <Button
                    type="submit"
                    disabled={!username.trim() || !password.trim()}
                    className="w-full mt-1 rounded-xl shadow-sm"
                    aria-busy={!username.trim() || !password.trim() ? undefined : false}
                >
                    Log In
                </Button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-600">
                Don’t have an account?{" "}
                <Link
                    to="/register"
                    className="font-semibold text-indigo-600 hover:text-indigo-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded"
                >
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

            <style>
                {`
          @keyframes fadeIn {
            from { opacity: 0; transform: translateY(6px); }
            to   { opacity: 1; transform: translateY(0); }
          }
          @keyframes riseIn {
            from { opacity: 0; transform: translateY(8px) scale(0.99); }
            to   { opacity: 1; transform: translateY(0) scale(1); }
          }
        `}
            </style>
        </AuthFormWrapper>
    );
};

export default Login;
