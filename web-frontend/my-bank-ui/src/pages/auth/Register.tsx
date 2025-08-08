import React, { useState } from "react";
import InputField from "../../components/InputField";
import Button from "../../components/Button";
import { Link } from "react-router-dom";
import { useNavigate } from "react-router-dom";
import AuthFormWrapper from "../../components/AuthFormWrapper";
import { useEmailValidation } from "../../hooks/useEmailValidation";

import { useUsernameAvailability } from "../../hooks/useUsernameAvailability";
import { usePasswordStrength } from "../../hooks/usePasswordStrength";
import { useConfirmPasswordMatch } from "../../hooks/useConfirmPasswordMatch";
import {useAlert} from "../../contexts/AlertContext.tsx";

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
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(dataToSend)
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
            <h2 className="text-3xl font-semibold text-center text-blue-800 mb-6">Join With US</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
                <InputField
                    label="User Name"
                    type="text"
                    name="username"
                    value={formData.fullName}
                    onChange={handleChange}
                    placeholder="e.g. Gimesha"
                />
                {formData.username && (
                    <p className={`text-sm mt-1 ${
                        checkingUsername ? "text-gray-500" : usernameAvailable ? "text-green-600" : "text-red-600"
                    }`}>
                        {checkingUsername
                            ? "Checking username..."
                            : usernameAvailable
                                ? "Username is available"
                                : "Username is not available or too short"}
                    </p>
                )}

                <InputField
                    label="Email"
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="you@example.com"
                />

                {formData.email && (
                    <p className={`text-sm mt-1 ${
                        emailValid === null
                            ? "text-gray-500"
                            : emailValid
                                ? "text-green-600"
                                : "text-red-600"
                    }`}>
                        {emailValid === null
                            ? ""
                            : emailValid
                                ? "Valid email address"
                                : "Invalid email address"}
                    </p>
                )}

                <InputField
                    label="Password"
                    type="password"
                    name="password"
                    value={formData.password}
                    onChange={handleChange}
                    placeholder="••••••••"
                />
                {formData.password && (
                    <ul className="text-sm mt-1 text-red-600 list-disc list-inside">
                        {passwordIssues.length === 0
                            ? <li className="text-green-600">Strong password!</li>
                            : passwordIssues.map((issue, idx) => (
                                <li key={idx}>{issue}</li>
                            ))
                        }
                    </ul>
                )}

                <InputField
                    label="Confirm Password"
                    type="password"
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    placeholder="••••••••"
                />
                {formData.confirmPassword && (
                    <p className={`text-sm mt-1 ${
                        passwordsMatch ? "text-green-600" : "text-red-600"
                    }`}>
                        {passwordsMatch ? "Passwords match" : "Passwords do not match"}
                    </p>
                )}

                <Button type="submit" className="w-full mt-4" disabled={!usernameAvailable || passwordScore < 4 || !passwordsMatch || !emailValid}>
                    Register
                </Button>
            </form>
            <p className="mt-6 text-center text-sm text-gray-600 dark:text-gray-400">
                Already have an account?{" "}
                <Link
                    to="/login"
                    className="text-indigo-600 hover:text-indigo-800 dark:hover:text-indigo-400 font-semibold"
                >
                    Login here
                </Link>
            </p>
        </AuthFormWrapper>
    );
};

export default Register;
