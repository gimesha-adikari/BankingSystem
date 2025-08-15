import React, { useEffect, useMemo, useRef, useState } from "react";
import { useAlert } from "@/contexts/use-alert";
import { useAuth } from "@/contexts/auth-context";
import InputField from "../components/InputField";
import PasswordVerification from "./PasswordVerification";
import api, { type NormalizedError } from "@/api/axios";

interface Props {
    onClose: () => void;
}

type Step = 1 | 2;

function scorePassword(pw: string): number {
    let score = 0;
    if (pw.length >= 8) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[a-z]/.test(pw)) score++;
    if (/\d/.test(pw)) score++;
    if (/[^A-Za-z0-9]/.test(pw)) score++;
    return Math.min(score, 4);
}

function strengthLabel(score: number) {
    switch (score) {
        case 0:
        case 1:
            return "Weak";
        case 2:
            return "Fair";
        case 3:
            return "Good";
        default:
            return "Strong";
    }
}

export default function ChangePasswordModal({ onClose }: Props) {
    const [step, setStep] = useState<Step>(1);
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmNewPassword, setConfirmNewPassword] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const { showAlert } = useAlert();
    const { token } = useAuth();

    const dialogRef = useRef<HTMLDivElement>(null);

    const mismatch = confirmNewPassword.length > 0 && newPassword !== confirmNewPassword;

    const complexityIssues = useMemo(() => {
        const issues: string[] = [];
        if (newPassword.length < 8) issues.push("At least 8 characters");
        if (!/[A-Z]/.test(newPassword)) issues.push("One uppercase letter");
        if (!/[a-z]/.test(newPassword)) issues.push("One lowercase letter");
        if (!/\d/.test(newPassword)) issues.push("One number");
        if (!/[^A-Za-z0-9]/.test(newPassword)) issues.push("One special character");
        return issues;
    }, [newPassword]);

    const strength = scorePassword(newPassword);

    useEffect(() => {
        const onKey = (e: KeyboardEvent) => {
            if (e.key === "Escape") onClose();
        };
        window.addEventListener("keydown", onKey);
        return () => window.removeEventListener("keydown", onKey);
    }, [onClose]);

    const onBackdropMouseDown = (e: React.MouseEvent) => {
        if (e.target === dialogRef.current) onClose();
    };

    const handlePasswordVerified = (password: string) => {
        setCurrentPassword(password);
        setStep(2);
    };

    const handleFinalSubmit = async () => {
        if (!newPassword || !confirmNewPassword) {
            showAlert("Please fill in both password fields.", "error");
            return;
        }
        if (mismatch) {
            showAlert("Passwords do not match.", "error");
            return;
        }
        if (complexityIssues.length > 0) {
            showAlert("Please satisfy the password requirements.", "error");
            return;
        }

        try {
            setSubmitting(true);
            await api.put("/api/v1/auth/change-password", {
                currentPassword,
                newPassword,
                confirmNewPassword,
            });
            showAlert("Password changed successfully.", "success");
            onClose();
        } catch (err) {
            const e = err as NormalizedError;
            showAlert(e.message || "Failed to change password.", "error");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div
            ref={dialogRef}
            onMouseDown={onBackdropMouseDown}
            className="fixed inset-0 z-50 flex items-center justify-center px-4
                 bg-[radial-gradient(80%_60%_at_50%_0%,rgba(99,102,241,0.18),transparent)]
                 bg-black/40 backdrop-blur-md"
            role="dialog"
            aria-modal="true"
            aria-labelledby="change-password-title"
        >
            <div
                className="w-full max-w-md rounded-2xl
                   bg-white/90 dark:bg-neutral-900/85 backdrop-blur
                   shadow-2xl ring-1 ring-black/5 dark:ring-white/10
                   px-6 py-6
                   transition
                   motion-safe:animate-[modalIn_.22s_ease-out]
                   focus:outline-none"
                onMouseDown={(e) => e.stopPropagation()}
            >
                {step === 1 ? (
                    <PasswordVerification token={token ?? ""} onVerified={handlePasswordVerified} onCancel={onClose} />
                ) : (
                    <>
                        <h2
                            id="change-password-title"
                            className="text-xl font-semibold text-slate-900 dark:text-white mb-1 tracking-tight"
                        >
                            Set New Password
                        </h2>
                        <p className="text-sm text-slate-600 dark:text-neutral-300 mb-4">
                            Use at least 8 characters with a mix of letters, numbers, and symbols.
                        </p>

                        <div className="space-y-4">
                            <InputField
                                label="New Password"
                                type="password"
                                name="newPassword"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                placeholder="••••••••"
                                autoFocus
                                error={undefined}
                                tone="light"
                            />

                            <div aria-live="polite">
                                <div className="h-2 rounded-full bg-slate-200 dark:bg-neutral-800 overflow-hidden ring-1 ring-black/5 dark:ring-white/10">
                                    <div
                                        className={[
                                            "h-full transition-all rounded-full",
                                            strength >= 1 ? "w-1/4" : "w-0",
                                            strength === 1 && "bg-rose-500",
                                            strength === 2 && "w-2/4 bg-amber-500",
                                            strength === 3 && "w-3/4 bg-emerald-500",
                                            strength === 4 && "w-full bg-emerald-600",
                                        ]
                                            .filter(Boolean)
                                            .join(" ")}
                                    />
                                </div>
                                <div className="mt-1 text-xs font-medium text-slate-700 dark:text-neutral-300">
                                    {strengthLabel(strength)}
                                </div>
                            </div>

                            <InputField
                                label="Confirm New Password"
                                type="password"
                                name="confirmNewPassword"
                                value={confirmNewPassword}
                                onChange={(e) => setConfirmNewPassword(e.target.value)}
                                placeholder="••••••••"
                                error={mismatch ? "Passwords do not match" : undefined}
                                tone="light"
                            />

                            {complexityIssues.length > 0 && (
                                <ul className="text-xs text-rose-600 dark:text-rose-400 space-y-1 list-disc pl-5">
                                    {complexityIssues.map((iss) => (
                                        <li key={iss}>{iss}</li>
                                    ))}
                                </ul>
                            )}
                        </div>

                        <div className="mt-6 flex justify-end gap-2">
                            <button
                                onClick={onClose}
                                className="px-4 py-2 rounded-xl
                           bg-slate-100 text-slate-800
                           hover:bg-slate-200
                           focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500
                           transition"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleFinalSubmit}
                                disabled={submitting}
                                className={[
                                    "px-4 py-2 rounded-xl text-white transition shadow-sm",
                                    "focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500",
                                    submitting
                                        ? "bg-indigo-400 cursor-not-allowed"
                                        : "bg-indigo-600 hover:bg-indigo-700",
                                ].join(" ")}
                            >
                                {submitting ? "Changing..." : "Change Password"}
                            </button>
                        </div>
                    </>
                )}
            </div>

            <style>
                {`
          @keyframes modalIn {
            from { opacity: 0; transform: translateY(6px) scale(0.98); }
            to   { opacity: 1; transform: translateY(0) scale(1); }
          }
        `}
            </style>
        </div>
    );
}
