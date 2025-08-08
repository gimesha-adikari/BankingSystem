import { useLocation, useNavigate } from "react-router-dom";
import React, { useState, useEffect } from "react";
import { usePasswordStrength } from "../../hooks/usePasswordStrength";
import { useConfirmPasswordMatch } from "../../hooks/useConfirmPasswordMatch";
import InputField from "../../components/InputField.tsx";

function useQuery() {
    return new URLSearchParams(useLocation().search);
}

const ResetPasswordPage = () => {
    const query = useQuery();
    const token = query.get("token");
    const navigate = useNavigate();

    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [message, setMessage] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [submitted, setSubmitted] = useState(false);

    const { score: passwordScore, issues: passwordIssues } = usePasswordStrength(newPassword);
    const passwordsMatch = useConfirmPasswordMatch(newPassword, confirmPassword);

    useEffect(() => {
        if (!token) {
            setMessage("Missing token in URL");
            return;
        }

        fetch(`/api/v1/auth/reset-password/${token}`)
            .then(res => {
                if (!res.ok) throw new Error("Invalid or expired token");
                return res.text();
            })
            .then(msg => setMessage(msg))
            .catch(err => setMessage(err.message));
    }, [token]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!token) {
            setMessage("Token missing");
            return;
        }

        if (passwordScore < 4) {
            setMessage("Password is too weak");
            return;
        }

        if (!passwordsMatch) {
            setMessage("Passwords do not match");
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            const response = await fetch(`/api/v1/auth/reset-password?token=${token}&newPassword=${encodeURIComponent(newPassword)}`, {
                method: "POST",
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(errorText || "Password reset failed");
            }

            setSubmitted(true);
            setMessage("Password has been reset. Redirecting to login...");
            setTimeout(() => navigate("/login"), 3000);
        } catch (err: any) {
            setMessage(err.message);
        } finally {
            setLoading(false);
        }
    };

    if (submitted) {
        return (
            <div className="flex items-center justify-center h-screen bg-gray-100">
                <div className="bg-white p-6 rounded shadow text-center">
                    <h2 className="text-xl font-semibold mb-4">Password Reset Successful</h2>
                    <p>{message}</p>
                </div>
            </div>
        );
    }

    return (
        <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-blue-100 to-blue-300">
            <form onSubmit={handleSubmit} className="bg-white p-8 rounded shadow-md w-full max-w-md">
                <h2 className="text-2xl font-semibold mb-4 text-center">Reset Your Password</h2>

                {message && <div className="text-sm mb-4 text-red-600 text-center">{message}</div>}

                <div className="mb-4">
                    <InputField
                        label="New Password"
                        type="password"
                        name="password"
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="••••••••"
                    />
                    {newPassword && (
                        <ul className="text-sm mt-1 text-red-600 list-disc list-inside">
                            {passwordIssues.length === 0
                                ? <li className="text-green-600">Strong password!</li>
                                : passwordIssues.map((issue, idx) => (
                                    <li key={idx}>{issue}</li>
                                ))
                            }
                        </ul>
                    )}
                </div>

                <div className="mb-6">
                    <InputField
                        label="Confirm Password"
                        type="password"
                        name="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="••••••••"
                    />
                    {confirmPassword && (
                        <p className={`text-sm mt-1 ${
                            passwordsMatch ? "text-green-600" : "text-red-600"
                        }`}>
                            {passwordsMatch ? "Passwords match" : "Passwords do not match"}
                        </p>
                    )}
                </div>

                <button
                    type="submit"
                    disabled={loading || passwordScore < 4 || !passwordsMatch}
                    className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
                >
                    {loading ? "Submitting..." : "Reset Password"}
                </button>
            </form>
        </div>
    );
};

export default ResetPasswordPage;
