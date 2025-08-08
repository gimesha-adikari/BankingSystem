import React from "react";

const AuthFormWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    return (
        <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 to-blue-100 px-4">
            <div className="max-w-md w-full bg-white p-8 rounded-lg shadow-lg">
                {children}
            </div>
        </div>
    );
};

export default AuthFormWrapper;
