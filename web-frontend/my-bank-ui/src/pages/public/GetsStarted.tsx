import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

const GetsStarted = () => {
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem("token");
        if (token) navigate("/customer/home");
    }, [navigate]);

    return (
        <div className="min-h-screen grid place-items-center text-indigo-100 bg-[#0B0B12] bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)] px-6">
            <div className="w-full max-w-2xl rounded-3xl ring-1 ring-white/10 bg-white/5 backdrop-blur p-8 md:p-12 shadow-[0_10px_40px_rgba(0,0,0,0.35)] motion-safe:animate-[riseIn_.35s_ease-out] text-center">
                <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight text-white">
                    Welcome to MyBank
                </h1>
                <p className="mt-3 text-base md:text-lg text-indigo-300/90">
                    Please log in or create an account to continue.
                </p>

                <div className="mt-8 flex flex-col sm:flex-row items-stretch sm:items-center justify-center gap-3">
                    <a
                        href="/login"
                        className="inline-flex items-center justify-center rounded-xl px-6 py-3 bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                    >
                        Login
                    </a>
                    <a
                        href="/register"
                        className="inline-flex items-center justify-center rounded-xl px-6 py-3 bg-emerald-600 text-white hover:bg-emerald-700 transition shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500"
                    >
                        Register
                    </a>
                </div>
            </div>

            <style>
                {`
          @keyframes riseIn {
            from { opacity: 0; transform: translateY(8px) scale(0.99); }
            to   { opacity: 1; transform: translateY(0) scale(1); }
          }
        `}
            </style>
        </div>
    );
};

export default GetsStarted;
