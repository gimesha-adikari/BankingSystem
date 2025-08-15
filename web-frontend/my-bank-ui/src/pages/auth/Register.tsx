import React, { useState } from "react";
import InputField from "../../components/InputField";
import Button from "../../components/Button";
import { Link, useNavigate } from "react-router-dom";
import AuthFormWrapper from "../../components/AuthFormWrapper";
import { useEmailValidation } from "@/hooks/useEmailValidation.ts";
import { useUsernameAvailability } from "@/hooks/useUsernameAvailability.ts";
import { usePasswordStrength } from "@/hooks/usePasswordStrength.ts";
import { useConfirmPasswordMatch } from "@/hooks/useConfirmPasswordMatch.ts";
import { useAlert } from "@/contexts/use-alert";

const Register = () => {
    const [formData, setFormData] = useState({
        username: "",
        email: "",
        password: "",
        confirmPassword: "",
    });

    const { showAlert } = useAlert();

    const { available: usernameAvailable, loading: checkingUsername } = useUsernameAvailability(formData.username);
    const { score: passwordScore, issues: passwordIssues } = usePasswordStrength(formData.password);
    const passwordsMatch = useConfirmPasswordMatch(formData.password, formData.confirmPassword);
    const emailValid = useEmailValidation(formData.email);

    const navigate = useNavigate();

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!usernameAvailable) {
            showAlert("Username is not available or too short", "error");
            return;
        }
        if (passwordScore < 4) {
            showAlert("Password is too weak", "error");
            return;
        }
        if (!passwordsMatch) {
            showAlert("Passwords do not match", "error");
            return;
        }

        try {
            const { confirmPassword, ...dataToSend } = formData;

            const response = await fetch("/api/v1/auth/register", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(dataToSend),
            });

            if (response.status === 201) {
                const msg = await response.text();
                showAlert(msg, "success");
                navigate("/login");
            } else if (response.status === 409) {
                const msg = await response.text();
                showAlert(msg, "error");
            } else {
                const msg = await response.text();
                showAlert(msg, "error");
            }
        } catch (error) {
            showAlert("Network error, please try again later.", "error");
            console.error("Registration error:", error);
        }
    };

    return (
        <AuthFormWrapper>
            <div className="mx-auto flex flex-col items-center gap-2 text-center motion-safe:animate-[fadeIn_.25s_ease-out]">
                <div className="h-11 w-11 rounded-2xl bg-indigo-600/15 ring-1 ring-indigo-500/30 grid place-items-center">
                    <span className="text-xl">📝</span>
                </div>
                <h2 className="text-2xl md:text-3xl font-semibold tracking-tight text-slate-950 dark:text-white">
                    Create your account
                </h2>
                <p className="text-sm text-slate-700 dark:text-indigo-200">
                    Join MyBank to manage and monitor your finances securely.
                </p>
            </div>

            <form onSubmit={handleSubmit} className="mt-6 space-y-5 motion-safe:animate-[riseIn_.28s_ease-out]">
                <div>
                    <InputField
                        label="User Name"
                        type="text"
                        name="username"
                        value={formData.username}
                        onChange={handleChange}
                        placeholder="e.g. Gimesha"
                        required
                        tone="light"
                    />
                    {formData.username && (
                        <div
                            className={[
                                "mt-1 inline-flex items-center rounded-md px-2 py-1 text-xs ring-1",
                                checkingUsername
                                    ? "text-slate-600 bg-slate-100 ring-slate-200"
                                    : usernameAvailable
                                        ? "text-emerald-700 bg-emerald-50 ring-emerald-200"
                                        : "text-rose-700 bg-rose-50 ring-rose-200",
                            ].join(" ")}
                            aria-live="polite"
                        >
                            {checkingUsername
                                ? "Checking username…"
                                : usernameAvailable
                                    ? "Username is available"
                                    : "Username is not available or too short"}
                        </div>
                    )}
                </div>

                <div>
                    <InputField
                        label="Email"
                        type="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        placeholder="you@example.com"
                        required
                        tone="light"
                    />
                    {formData.email && (
                        <div
                            className={[
                                "mt-1 inline-flex items-center rounded-md px-2 py-1 text-xs ring-1",
                                emailValid === null
                                    ? "text-slate-600 bg-slate-100 ring-slate-200"
                                    : emailValid
                                        ? "text-emerald-700 bg-emerald-50 ring-emerald-200"
                                        : "text-rose-700 bg-rose-50 ring-rose-200",
                            ].join(" ")}
                            aria-live="polite"
                        >
                            {emailValid === null ? "" : emailValid ? "Valid email address" : "Invalid email address"}
                        </div>
                    )}
                </div>

                <div>
                    <InputField
                        label="Password"
                        type="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder="••••••••"
                        required
                        tone="light"
                    />
                    <div className="mt-2">
                        <div className="h-2 rounded-full bg-slate-200 overflow-hidden ring-1 ring-black/5">
                            <div
                                className={[
                                    "h-full rounded-full transition-all",
                                    passwordScore >= 1 ? "w-1/4" : "w-0",
                                    passwordScore === 1 && "bg-rose-500",
                                    passwordScore === 2 && "w-2/4 bg-amber-500",
                                    passwordScore === 3 && "w-3/4 bg-emerald-500",
                                    passwordScore >= 4 && "w-full bg-emerald-600",
                                ]
                                    .filter(Boolean)
                                    .join(" ")}
                            />
                        </div>
                        {formData.password && (
                            <div className="mt-2">
                                {passwordIssues.length === 0 ? (
                                    <span className="text-xs text-emerald-700 bg-emerald-50 ring-1 ring-emerald-200 px-2 py-1 rounded-md">
                    Strong password
                  </span>
                                ) : (
                                    <ul className="text-xs text-rose-700 bg-rose-50 ring-1 ring-rose-200 rounded-md px-3 py-2 space-y-1">
                                        {passwordIssues.map((issue, idx) => (
                                            <li key={idx}>{issue}</li>
                                        ))}
                                    </ul>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                <div>
                    <InputField
                        label="Confirm Password"
                        type="password"
                        name="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        placeholder="••••••••"
                        required
                        tone="light"
                    />
                    {formData.confirmPassword && (
                        <div
                            className={[
                                "mt-1 inline-flex items-center rounded-md px-2 py-1 text-xs ring-1",
                                passwordsMatch
                                    ? "text-emerald-700 bg-emerald-50 ring-emerald-200"
                                    : "text-rose-700 bg-rose-50 ring-rose-200",
                            ].join(" ")}
                            aria-live="polite"
                        >
                            {passwordsMatch ? "Passwords match" : "Passwords do not match"}
                        </div>
                    )}
                </div>

                <Button
                    type="submit"
                    className="w-full mt-2 rounded-xl shadow-sm"
                    disabled={!usernameAvailable || passwordScore < 4 || !passwordsMatch || !emailValid}
                >
                    Register
                </Button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-600">
                Already have an account?{" "}
                <Link
                    to="/login"
                    className="font-semibold text-indigo-600 hover:text-indigo-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded"
                >
                    Login here
                </Link>
            </p>

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

export default Register;
