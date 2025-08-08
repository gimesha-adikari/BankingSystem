import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

const GetsStarted = () => {
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem("token"); // Adjust based on your actual storage
        if (token) {
            navigate("/customer/home");
        }
    }, [navigate]);

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-gradient-to-br from-blue-100 to-blue-300 text-center px-4">
            <h1 className="text-4xl font-bold mb-4 text-blue-800">Welcome to Our App</h1>
            <p className="text-lg text-gray-700 mb-6">Please login or register to continue</p>
            <div className="space-x-4">
                <a
                    href="/login"
                    className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 transition"
                >
                    Login
                </a>
                <a
                    href="/register"
                    className="bg-green-600 text-white px-6 py-2 rounded hover:bg-green-700 transition"
                >
                    Register
                </a>
            </div>
        </div>
    );
};

export default GetsStarted;
