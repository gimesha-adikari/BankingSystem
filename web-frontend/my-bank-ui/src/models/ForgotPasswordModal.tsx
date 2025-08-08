import React from "react";
import InputField from "../components/InputField";

interface ForgotPasswordModalProps {
    isOpen: boolean;
    email: string;
    onEmailChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    onSubmit: () => void;
    onClose: () => void;
}

const ForgotPasswordModal: React.FC<ForgotPasswordModalProps> = ({
                                                                     isOpen,
                                                                     email,
                                                                     onEmailChange,
                                                                     onSubmit,
                                                                     onClose,
                                                                 }) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 bg-black/30 backdrop-blur-sm flex items-center justify-center">
            <div className="bg-white p-6 rounded-lg shadow-lg max-w-sm w-full">
                <h3 className="text-xl font-semibold mb-4">Forgot Password</h3>
                <InputField
                    label="Email"
                    type="email"
                    value={email}
                    onChange={onEmailChange}
                    placeholder="Enter your email"
                    required
                />
                <div className="mt-4 flex justify-end space-x-2">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 bg-gray-300 rounded hover:bg-gray-400"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={onSubmit}
                        className="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700"
                    >
                        Submit
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ForgotPasswordModal;
