import React, { useEffect, useRef, useState } from "react";
import type { ChangeEvent, MouseEvent } from "react";
import InputField from "../components/InputField";
import api, { type NormalizedError } from "@/api/axios";
import { useAlert } from "@/contexts/use-alert";

interface ForgotPasswordModalProps {
    isOpen: boolean;
    email: string;
    onEmailChange: (e: ChangeEvent<HTMLInputElement>) => void;
    onSubmit?: () => void | Promise<void>;
    onClose: () => void;
}

export default function ForgotPasswordModal({
                                                isOpen,
                                                email,
                                                onEmailChange,
                                                onSubmit,
                                                onClose,
                                            }: ForgotPasswordModalProps) {
    const [error, setError] = useState<string | null>(null);
    const [submitting, setSubmitting] = useState(false);
    const backdropRef = useRef<HTMLDivElement>(null);
    const titleId = "forgot-password-title";
    const descId = "forgot-password-desc";
    const { showAlert } = useAlert();

    useEffect(() => {
        if (!isOpen) return;
        const onKeyDown = (e: KeyboardEvent) => {
            if (e.key === "Escape") onClose();
        };
        window.addEventListener("keydown", onKeyDown);
        return () => window.removeEventListener("keydown", onKeyDown);
    }, [isOpen, onClose]);

    useEffect(() => {
        if (isOpen) {
            setError(null);
            setSubmitting(false);
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const handleBackdrop = (e: MouseEvent<HTMLDivElement>) => {
        if (e.target === backdropRef.current) onClose();
    };

    const isValidEmail = (val: string) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val.trim());

    const handleSubmit = async () => {
        if (submitting) return;
        const trimmed = email.trim();
        if (!isValidEmail(trimmed)) {
            setError("Please enter a valid email address.");
            return;
        }
        setError(null);

        try {
            setSubmitting(true);
            await api.post("/api/v1/auth/forgot-password", { email: trimmed });
            showAlert("If an account exists, a reset link has been sent.", "success");
            await onSubmit?.();
            onClose();
        } catch (err) {
            const e = err as NormalizedError;
            const msg = e.message || "Failed to send reset link.";
            setError(msg);
            showAlert(msg, "error");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div
            ref={backdropRef}
            onMouseDown={handleBackdrop}
            className="fixed inset-0 z-50 flex items-center justify-center px-4
                 bg-[radial-gradient(80%_60%_at_50%_0%,rgba(99,102,241,0.18),transparent)]
                 bg-black/40 backdrop-blur-md"
            role="dialog"
            aria-modal="true"
            aria-labelledby={titleId}
            aria-describedby={descId}
        >
            <div
                className="w-full max-w-sm rounded-2xl
                   bg-white/90 dark:bg-neutral-900/85 backdrop-blur
                   shadow-2xl ring-1 ring-black/5 dark:ring-white/10
                   px-6 py-6 transition
                   motion-safe:animate-[modalIn_.22s_ease-out]"
                onMouseDown={(e) => e.stopPropagation()}
            >
                <div className="flex items-start justify-between gap-4">
                    <h3 id={titleId} className="text-xl font-semibold text-slate-900 dark:text-white tracking-tight">
                        Forgot Password
                    </h3>
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-md p-1.5 text-slate-500 dark:text-neutral-400
                       hover:text-slate-800 dark:hover:text-neutral-100
                       hover:bg-black/5 dark:hover:bg-white/10
                       focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500
                       transition"
                        aria-label="Close dialog"
                    >
                        ✕
                    </button>
                </div>

                <p id={descId} className="mt-2 text-sm text-slate-600 dark:text-neutral-300">
                    Enter the email linked to your account. We’ll send a reset link if it exists.
                </p>

                <form
                    className="mt-4 space-y-4"
                    onSubmit={(e) => {
                        e.preventDefault();
                        void handleSubmit();
                    }}
                >
                    <InputField
                        label="Email"
                        type="email"
                        name="email"
                        value={email}
                        onChange={(e) => {
                            setError(null);
                            onEmailChange(e);
                        }}
                        placeholder="you@example.com"
                        autoComplete="email"
                        autoFocus
                        error={error ?? undefined}
                        tone="light"
                    />

                    <div className="mt-2 flex justify-end gap-2">
                        <button
                            type="button"
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
                            type="submit"
                            disabled={submitting}
                            aria-busy={submitting || undefined}
                            className={[
                                "px-4 py-2 rounded-xl text-white transition shadow-sm",
                                "focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500",
                                submitting ? "bg-indigo-400 cursor-not-allowed" : "bg-indigo-600 hover:bg-indigo-700",
                            ].join(" ")}
                        >
                            {submitting ? "Sending…" : "Submit"}
                        </button>
                    </div>
                </form>

                {error && (
                    <div className="mt-2 text-sm text-rose-600 dark:text-rose-400" role="alert" aria-live="polite">
                        {error}
                    </div>
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
