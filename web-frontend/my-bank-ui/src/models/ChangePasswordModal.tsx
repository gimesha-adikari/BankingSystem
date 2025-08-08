// src/models/ChangePasswordModal.tsx
import React, { useState } from "react";
import { useAlert } from "../contexts/AlertContext";
import InputField from "../components/InputField";
import PasswordVerification from "./PasswordVerification";

interface Props {
    onClose: () => void;
}

export default function ChangePasswordModal({ onClose }: Props) {
    const [step, setStep] = useState<1 | 2>(1);
    const [currentPassword, setCurrentPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmNewPassword, setConfirmNewPassword] = useState("");

    const { showAlert } = useAlert();
    const token = localStorage.getItem("token");

    const handlePasswordVerified = (password: string) => {
        setCurrentPassword(password);
        setStep(2);
    };

    const handleFinalSubmit = async () => {
        if (!newPassword || !confirmNewPassword) {
            showAlert("Please fill in both password fields.", "error");
            return;
        }

        if (newPassword !== confirmNewPassword) {
            showAlert("Passwords do not match.", "error");
            return;
        }

        try {
            const res = await fetch("/api/v1/auth/change-password", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({
                    currentPassword,
                    newPassword,
                    confirmNewPassword,
                }),
            });

            if (res.ok) {
                showAlert("Password changed successfully.", "success");
                onClose();
            } else {
                const errorText = await res.text();
                showAlert(errorText || "Failed to change password.", "error");
            }
        } catch {
            showAlert("Network error. Please try again.", "error");
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm bg-white/30">
            <div className="bg-white p-6 rounded-xl shadow-lg w-full max-w-sm">
                {step === 1 ? (
                    <PasswordVerification token={token} onVerified={handlePasswordVerified} onCancel={onClose} />
                ) : (
                    <>
                        <h2 className="text-xl font-semibold mb-4">Set New Password</h2>
                        <div className="mb-4">
                            <InputField
                                label="New Password"
                                type="password"
                                value={newPassword}
                                onChange={(e) => setNewPassword(e.target.value)}
                                placeholder="••••••••"
                                autoFocus
                            />
                            <InputField
                                label="Confirm New Password"
                                type="password"
                                value={confirmNewPassword}
                                onChange={(e) => setConfirmNewPassword(e.target.value)}
                                placeholder="••••••••"
                            />
                        </div>
                        <div className="flex justify-end gap-2">
                            <button onClick={onClose} className="px-4 py-2 bg-gray-300 rounded">
                                Cancel
                            </button>
                            <button onClick={handleFinalSubmit} className="px-4 py-2 bg-green-600 text-white rounded">
                                Change Password
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
