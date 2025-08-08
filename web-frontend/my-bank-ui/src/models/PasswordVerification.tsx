// src/models/CurrentPasswordVerificationStep.tsx
import React, { useState } from "react";
import InputField from "../components/InputField";
import { useAlert } from "../contexts/AlertContext";

interface Props {
    token: string | null;
    onVerified: (password: string) => void;  // Pass password back when verified
    onCancel: () => void;
}

export default function PasswordVerification({ token, onVerified, onCancel }: Props) {
    const [password, setPassword] = useState("");
    const { showAlert } = useAlert();

    const verifyPassword = async () => {
        if (!token) {
            showAlert("You must be logged in.", "error");
            return;
        }
        try {
            const res = await fetch("/api/v1/users/verify-password", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ password }),
            });
            if (res.ok) {
                onVerified(password);
            } else {
                showAlert("Incorrect password.", "error");
            }
        } catch {
            showAlert("Failed to verify password.", "error");
        }
    };

    return (
        <>
            <h2 className="text-xl font-semibold mb-4">Confirm Current Password</h2>
            <div className="mb-4">
                <InputField
                    label="Current Password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    autoFocus
                />
            </div>
            <div className="flex justify-end gap-2">
                <button onClick={onCancel} className="px-4 py-2 bg-gray-300 rounded">
                    Cancel
                </button>
                <button
                    onClick={verifyPassword}
                    disabled={!password.trim()}
                    className="px-4 py-2 bg-blue-600 text-white rounded disabled:opacity-50"
                >
                    Confirm
                </button>
            </div>
        </>
    );
}
