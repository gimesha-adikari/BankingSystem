import React from "react";

const AuthFormWrapper: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    return (
        <div className="
      min-h-screen flex items-center justify-center
      bg-gradient-to-br from-blue-50 via-slate-100 to-blue-100
      dark:from-slate-900 dark:via-slate-950 dark:to-slate-900
      px-4 py-8
    ">
            <div
                className="
          backdrop-blur-md bg-white/90 dark:bg-neutral-900/80
          rounded-2xl shadow-lg ring-1 ring-black/5 dark:ring-white/10
          w-full max-w-md px-8 py-10
          transition-all duration-300
          hover:shadow-xl hover:-translate-y-1
          focus-within:ring-2 focus-within:ring-indigo-500
        "
            >
                {children}
            </div>
        </div>
    );
};

export default AuthFormWrapper;
