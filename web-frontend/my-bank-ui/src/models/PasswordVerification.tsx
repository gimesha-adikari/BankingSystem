import { useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import InputField from "../components/InputField";
import { useAlert } from "@/contexts/use-alert";
import api, { type NormalizedError } from "@/api/axios";

interface Props {
    /** Optional: legacy; axios interceptor supplies Authorization header */
    token?: string | null;
    onVerified: (password: string) => void;
    onCancel: () => void;
}

export default function PasswordVerification({ onVerified, onCancel }: Props) {
    const [password, setPassword] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const { showAlert } = useAlert();

    const onChange = (e: ChangeEvent<HTMLInputElement>) => {
        setPassword(e.target.value);
        if (error) setError(null);
    };

    const onSubmit = async (e?: FormEvent) => {
        e?.preventDefault();
        const pw = password.trim();
        if (!pw) return;

        try {
            setSubmitting(true);
            setError(null);

            await api.post("/api/v1/users/verify-password", { password: pw });

            onVerified(pw);
        } catch (err) {
            const n = err as NormalizedError;
            const msg = n.message || "Incorrect password.";
            setError(msg);
            showAlert(msg, "error");
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <form onSubmit={onSubmit}>
            <h2 className="text-xl font-semibold mb-2">Confirm Current Password</h2>
            <p className="text-sm text-gray-600 mb-4">
                For your security, please confirm your current password to continue.
            </p>

            <div className="mb-4">
                <InputField
                    label="Current Password"
                    type="password"
                    name="currentPassword"
                    value={password}
                    onChange={onChange}
                    placeholder="••••••••"
                    autoFocus
                    autoComplete="current-password"
                    error={error ?? undefined}
                />
                {error && (
                    <div className="mt-1 text-sm text-rose-600" role="alert" aria-live="polite">
                        {error}
                    </div>
                )}
            </div>

            <div className="flex justify-end gap-2">
                <button
                    type="button"
                    onClick={onCancel}
                    disabled={submitting}
                    className="px-4 py-2 rounded-lg bg-gray-100 text-gray-700 hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-60"
                >
                    Cancel
                </button>
                <button
                    type="submit"
                    disabled={submitting || password.trim().length === 0}
                    className={[
                        "px-4 py-2 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500",
                        submitting || password.trim().length === 0
                            ? "bg-indigo-400 cursor-not-allowed"
                            : "bg-indigo-600 hover:bg-indigo-700",
                    ].join(" ")}
                >
                    {submitting ? "Verifying…" : "Confirm"}
                </button>
            </div>
        </form>
    );
}
