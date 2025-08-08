import React from "react";
import { useNavigate } from "react-router-dom";

const Unauthorized = () => {
    const navigate = useNavigate();

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-gradient-to-tr from-purple-600 via-indigo-700 to-blue-800 px-6">
            <div className="bg-white bg-opacity-90 backdrop-blur-lg rounded-3xl shadow-xl max-w-md w-full p-10 text-center">
                <h1 className="text-9xl font-extrabold text-red-500 animate-pulse mb-4 select-none">403</h1>
                <h2 className="text-3xl font-semibold text-gray-800 mb-4">Unauthorized Access</h2>
                <p className="text-gray-600 mb-8 leading-relaxed">
                    Sorry, you don’t have permission to access this page. Please contact the administrator if you believe this is a mistake.
                </p>
                <p className="mt-6 text-center text-sm text-gray-600">
                    Go to Homepage{" "}
                    <a href="/" className="text-indigo-600 hover:text-indigo-800 font-semibold">
                        Homepage
                    </a>
                </p>
            </div>
        </div>

    );
};

export default Unauthorized;
