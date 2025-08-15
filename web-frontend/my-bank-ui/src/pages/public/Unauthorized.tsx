import React from "react";
import { useNavigate } from "react-router-dom";

const Unauthorized = () => {
    const navigate = useNavigate();

    return (
        <div className="relative min-h-screen grid place-items-center text-indigo-100 bg-[#0B0B12] overflow-hidden">
            <div aria-hidden className="pointer-events-none absolute -top-40 -left-20 h-[420px] w-[420px] rounded-full bg-indigo-600/20 blur-3xl" />
            <div aria-hidden className="pointer-events-none absolute -bottom-40 -right-20 h-[420px] w-[420px] rounded-full bg-rose-500/20 blur-3xl" />

            <div className="w-full max-w-xl rounded-3xl ring-1 ring-white/10 bg-white/5 backdrop-blur p-8 md:p-12 shadow-[0_10px_40px_rgba(0,0,0,0.35)] motion-safe:animate-[riseIn_.35s_ease-out] text-center">
                <div className="text-7xl md:text-8xl font-extrabold tracking-tight">
          <span className="bg-gradient-to-r from-rose-400 via-amber-300 to-indigo-300 bg-clip-text text-transparent drop-shadow">
            403
          </span>
                </div>
                <h2 className="mt-2 text-2xl md:text-3xl font-semibold text-white">Unauthorized</h2>
                <p className="mt-2 text-sm md:text-base text-indigo-200/80">
                    You don’t have permission to access this page. If you believe this is a mistake, contact your administrator.
                </p>

                <div className="mt-8 flex flex-col sm:flex-row gap-3 justify-center">
                    <button
                        onClick={() => navigate(-1)}
                        className="inline-flex items-center justify-center rounded-xl px-5 py-2.5 bg-white/10 text-white hover:bg-white/20 transition shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                    >
                        Go Back
                    </button>
                    <button
                        onClick={() => navigate("/")}
                        className="inline-flex items-center justify-center rounded-xl px-5 py-2.5 bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                    >
                        Homepage
                    </button>
                    <a
                        href="/login"
                        className="inline-flex items-center justify-center rounded-xl px-5 py-2.5 bg-rose-600 text-white hover:bg-rose-700 transition shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-500"
                    >
                        Sign In
                    </a>
                </div>
            </div>

            <style>
                {`
          @keyframes riseIn {
            from { opacity: 0; transform: translateY(8px) scale(0.995); }
            to   { opacity: 1; transform: translateY(0) scale(1); }
          }
        `}
            </style>
        </div>
    );
};

export default Unauthorized;
