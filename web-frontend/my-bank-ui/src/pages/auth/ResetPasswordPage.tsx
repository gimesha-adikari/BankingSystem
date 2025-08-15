import { useEffect, useMemo, useState } from "react";
import { useSearchParams, Link, useNavigate } from "react-router-dom";
import api from "@/api/axios";
import InputField from "@/components/InputField";
import { useAlert } from "@/contexts/use-alert";

export default function ResetPasswordPage() {
    const [sp] = useSearchParams();
    const token = sp.get("token")?.trim() ?? "";
    const navigate = useNavigate();
    const { showAlert } = useAlert();

    const [validating, setValidating] = useState(true);
    const [tokenError, setTokenError] = useState<string | null>(null);

    const [newPassword, setNewPassword] = useState("");
    const [confirm, setConfirm] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [done, setDone] = useState(false);

    const strengthError = useMemo(() => {
        const pw = newPassword;
        if (pw.length < 8) return "At least 8 characters required.";
        if (!/[!@#$%^&*(),.?\":{}|<>]/.test(pw)) return "Include at least one special character.";
        if (!/[0-9]/.test(pw)) return "Include at least one number.";
        if (!/[A-Z]/.test(pw)) return "Include at least one uppercase letter.";
        return null;
    }, [newPassword]);

    useEffect(() => {
        if (!token) {
            setTokenError("Missing reset token.");
            setValidating(false);
            return;
        }

        (async () => {
            try {
                setValidating(true);
                setTokenError(null);
                await api.get("/api/v1/auth/reset-password", { params: { token } });
            } catch (err: unknown) {
                const msg = (err as { message?: string })?.message || "This reset link is invalid or has expired.";
                setTokenError(msg);
            } finally {
                setValidating(false);
            }
        })();
    }, [token]);

    const submit = async () => {
        if (!token || validating || submitting) return;

        if (strengthError) {
            showAlert(strengthError, "error");
            return;
        }
        if (newPassword !== confirm) {
            showAlert("Passwords do not match.", "error");
            return;
        }

        try {
            setSubmitting(true);
            await api.post("/api/v1/auth/reset-password", null, {
                params: { token, newPassword },
            });

            setDone(true);
            showAlert("Password has been reset. You can now sign in.", "success");
        } catch (err: unknown) {
            const msg =
                (err as { message?: string })?.message ||
                "Failed to reset password. The link may be invalid or expired.";
            showAlert(msg, "error");
        } finally {
            setSubmitting(false);
        }
    };

    if (validating) {
        return (
            <div className="min-h-screen grid place-items-center px-4 bg-[#0E1220] bg-[radial-gradient(70%_50%_at_50%_-10%,rgba(99,102,241,0.18),transparent)]">
                <div className="max-w-md w-full rounded-2xl bg-white/90 backdrop-blur shadow-2xl ring-1 ring-black/5 p-8 motion-safe:animate-[fadeIn_.25s_ease-out]">
                    <div className="h-6 w-40 rounded bg-slate-200 mb-4 animate-pulse" />
                    <div className="space-y-3">
                        <div className="h-4 w-full rounded bg-slate-200 animate-pulse" />
                        <div className="h-4 w-5/6 rounded bg-slate-200 animate-pulse" />
                        <div className="h-10 w-28 rounded bg-slate-200 mt-4 animate-pulse" />
                    </div>
                </div>
            </div>
        );
    }

    if (tokenError) {
        return (
            <div className="min-h-screen grid place-items-center px-4 bg-[#0E1220] bg-[radial-gradient(70%_50%_at_50%_-10%,rgba(99,102,241,0.18),transparent)]">
                <div className="max-w-md w-full rounded-2xl bg-white/90 backdrop-blur shadow-2xl ring-1 ring-black/5 p-8 text-center motion-safe:animate-[riseIn_.28s_ease-out]">
                    <h1 className="text-xl font-semibold text-slate-950 mb-2">Reset link issue</h1>
                    <p className="text-slate-700">{tokenError}</p>
                    <div className="mt-6">
                        <Link
                            to="/login"
                            className="inline-flex items-center justify-center px-4 py-2 rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                        >
                            Back to Sign in
                        </Link>
                    </div>
                </div>
            </div>
        );
    }

    if (done) {
        return (
            <div className="min-h-screen grid place-items-center px-4 bg-[#0E1220] bg-[radial-gradient(70%_50%_at_50%_-10%,rgba(99,102,241,0.18),transparent)]">
                <div className="max-w-md w-full rounded-2xl bg-white/90 backdrop-blur shadow-2xl ring-1 ring-black/5 p-8 text-center motion-safe:animate-[riseIn_.28s_ease-out]">
                    <h1 className="text-xl font-semibold text-slate-950 mb-2">Password updated</h1>
                    <p className="text-slate-700">You can now sign in with your new password.</p>
                    <div className="mt-6 flex justify-center gap-2">
                        <button
                            onClick={() => navigate("/login")}
                            className="px-4 py-2 rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                        >
                            Go to Sign in
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen grid place-items-center px-4 bg-[#0E1220] bg-[radial-gradient(70%_50%_at_50%_-10%,rgba(99,102,241,0.18),transparent)]">
            <div className="max-w-md w-full rounded-2xl bg-white/90 backdrop-blur shadow-2xl ring-1 ring-black/5 p-8 motion-safe:animate-[riseIn_.28s_ease-out]">
                <h1 className="text-xl font-semibold text-slate-950">Set a new password</h1>
                <p className="text-sm text-slate-700 mt-1">Your reset token was validated. Choose a strong password.</p>

                <div className="mt-6 space-y-4">
                    <InputField
                        label="New password"
                        type="password"
                        name="newPassword"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="••••••••"
                        autoComplete="new-password"
                        error={undefined}
                        tone="light"
                    />
                    <InputField
                        label="Confirm password"
                        type="password"
                        name="confirmPassword"
                        value={confirm}
                        onChange={(e) => setConfirm(e.target.value)}
                        placeholder="••••••••"
                        autoComplete="new-password"
                        error={undefined}
                        tone="light"
                    />

                    {strengthError ? (
                        <p className="text-sm text-rose-600" role="alert" aria-live="polite">
                            {strengthError}
                        </p>
                    ) : newPassword ? (
                        <p className="text-sm text-emerald-600" aria-live="polite">
                            Looks good.
                        </p>
                    ) : null}

                    <div className="pt-2 flex justify-end gap-2">
                        <Link
                            to="/login"
                            className="px-4 py-2 rounded-xl bg-slate-100 text-slate-800 hover:bg-slate-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                        >
                            Cancel
                        </Link>
                        <button
                            onClick={submit}
                            disabled={submitting || !newPassword || !confirm || !!strengthError || newPassword !== confirm}
                            className={[
                                "px-4 py-2 rounded-xl text-white transition shadow-sm",
                                "focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500",
                                submitting || !newPassword || !confirm || !!strengthError || newPassword !== confirm
                                    ? "bg-indigo-400 cursor-not-allowed"
                                    : "bg-indigo-600 hover:bg-indigo-700",
                            ].join(" ")}
                        >
                            {submitting ? "Saving…" : "Update password"}
                        </button>
                    </div>
                </div>
            </div>

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
        </div>
    );
}
