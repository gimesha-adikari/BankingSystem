import { useState } from "react";
import Sidebar from "@/components/Sidebar";
import HomeCard from "@/components/HomeCard";
import { useAuth } from "@/contexts/auth-context";

const Home = () => {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const { user, isAuthenticated } = useAuth();

    return (
        <div className="flex h-screen text-indigo-100 bg-[#0B0B12] bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)]">
            <Sidebar isOpen={sidebarOpen} role={user?.role ?? "CUSTOMER"} />

            <div className="flex-1 flex flex-col">
                <header className="md:hidden sticky top-0 z-40 bg-white/5 backdrop-blur ring-1 ring-white/10 py-3 px-4">
                    <div className="flex justify-between items-center">
                        <button
                            onClick={() => setSidebarOpen(!sidebarOpen)}
                            aria-label="Toggle sidebar"
                            aria-expanded={sidebarOpen}
                            className="h-9 w-9 grid place-items-center rounded-xl text-indigo-300 hover:text-white hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                        >
                            ☰
                        </button>
                        <h1 className="text-base font-semibold text-indigo-200">Home</h1>
                    </div>
                </header>

                <main className="flex-1 px-6 py-6 md:px-8 md:py-8 overflow-auto">
                    <div className="max-w-7xl mx-auto">
                        {isAuthenticated ? (
                            <>
                                <div className="motion-safe:animate-[fadeIn_.35s_ease-out]">
                                    <h2 className="text-3xl md:text-4xl font-bold tracking-tight text-white">
                                        {`Welcome back, ${user?.username}!`}
                                    </h2>
                                    <p className="mt-1 text-sm text-indigo-300/80">
                                        Quick shortcuts to the things you do most.
                                    </p>
                                </div>

                                <div className="mt-6 grid gap-6 md:grid-cols-2 xl:grid-cols-3 motion-safe:animate-[riseIn_.35s_ease-out]">
                                    {[
                                        {
                                            title: "Accounts",
                                            description: "View balances and manage your bank accounts.",
                                            link: "/accounts",
                                            buttonText: "View Accounts",
                                        },
                                        {
                                            title: "Transactions",
                                            description: "Check your recent deposits, withdrawals, and transfers.",
                                            link: "/transactions",
                                            buttonText: "View Transactions",
                                        },
                                        {
                                            title: "Support",
                                            description: "Need help? Contact our support team.",
                                            link: "/support",
                                            buttonText: "Contact Support",
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
                                <h2 className="text-4xl font-bold mb-3 text-white">Welcome to MyBank</h2>
                                <p className="text-indigo-300 mb-8 max-w-md mx-auto">
                                    Secure, reliable, and convenient banking solutions tailored for you.
                                </p>
                                <a
                                    href="/register"
                                    className="inline-flex items-center justify-center gap-2 rounded-xl px-6 py-3
                             bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-sm
                             focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                                >
                                    Get Started
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

export default Home;
