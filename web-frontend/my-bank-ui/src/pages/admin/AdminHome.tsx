import { useState, useEffect } from "react";
import Sidebar from "../../components/Sidebar";
import HomeCard from "../../components/HomeCard";

const AdminHome = () => {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [username, setUsername] = useState("");

    useEffect(() => {
        const token = localStorage.getItem("token");
        setIsLoggedIn(!!token);
        if (token) {
            try {
                const payload = JSON.parse(atob(token.split(".")[1]));
                setUsername(payload.sub);
            } catch {
                console.error("Invalid token");
            }
        }
    }, []);

    return (
        <div className="flex h-screen bg-[#0B0B12] bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)]">
            <Sidebar isOpen={sidebarOpen} role="ADMIN" />

            <div className="flex-1 flex flex-col">
                <header className="md:hidden sticky top-0 z-40 bg-white/5 dark:bg-white/5 backdrop-blur ring-1 ring-white/10 py-3 px-4">
                    <div className="flex justify-between items-center">
                        <button
                            onClick={() => setSidebarOpen(!sidebarOpen)}
                            aria-label="Toggle sidebar"
                            aria-expanded={sidebarOpen}
                            className="h-9 w-9 grid place-items-center rounded-xl text-indigo-300 hover:text-white hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                        >
                            ☰
                        </button>
                        <h1 className="text-base font-semibold text-indigo-200">Admin Home</h1>
                    </div>
                </header>

                <main className="flex-1 px-6 py-6 md:px-8 md:py-8 overflow-auto">
                    <div className="max-w-7xl mx-auto text-indigo-100">
                        {isLoggedIn ? (
                            <>
                                <div className="mb-6 md:mb-8">
                                    <h2 className="text-3xl md:text-4xl font-bold tracking-tight text-white motion-safe:animate-[fadeIn_.35s_ease-out]">
                                        {`Welcome back, Admin ${username}!`}
                                    </h2>
                                    <p className="mt-1 text-sm text-indigo-300/80">
                                        Manage your institution, monitor performance, and keep everything running smoothly.
                                    </p>
                                </div>

                                <div className="grid gap-6 md:grid-cols-2 xl:grid-cols-3 motion-safe:animate-[riseIn_.35s_ease-out]">
                                    {[
                                        {
                                            title: "User Management",
                                            description: "Manage user accounts and roles.",
                                            link: "/admin/users",
                                            buttonText: "Manage Users",
                                        },
                                        {
                                            title: "Reports",
                                            description: "View system reports and analytics.",
                                            link: "/admin/reports",
                                            buttonText: "View Reports",
                                        },
                                        {
                                            title: "Settings",
                                            description: "Configure system settings and preferences.",
                                            link: "/admin/settings",
                                            buttonText: "Settings",
                                        },
                                    ].map((card, idx) => (
                                        <HomeCard
                                            key={idx}
                                            title={card.title}
                                            description={card.description}
                                            link={card.link}
                                            buttonText={card.buttonText}
                                        />
                                    ))}
                                </div>
                            </>
                        ) : (
                            <div className="text-center mt-20 motion-safe:animate-[fadeIn_.35s_ease-out]">
                                <h2 className="text-4xl font-bold mb-3 text-white">Welcome to MyBank Admin</h2>
                                <p className="text-indigo-300 mb-8 max-w-md mx-auto">
                                    Admin portal to manage the banking system efficiently.
                                </p>
                                <a
                                    href="/admin/login"
                                    className="inline-flex items-center justify-center gap-2 rounded-xl px-6 py-3
                             bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-sm
                             focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                                >
                                    Login
                                </a>
                            </div>
                        )}
                    </div>
                </main>
            </div>

            <style>
                {`
          @keyframes fadeIn {
            from { opacity: 0; transform: translateY(6px); }
            to   { opacity: 1; transform: translateY(0); }
          }
          @keyframes riseIn {
            from { opacity: 0; transform: translateY(8px) scale(0.99); }
            to   { opacity: 1; transform: translateY(0) scale(1); }
          }
        `}
            </style>
        </div>
    );
};

export default AdminHome;
