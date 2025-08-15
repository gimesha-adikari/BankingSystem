import { useState } from "react";
import Sidebar from "../../components/Sidebar";
import StatCard from "../../components/StatCard";

const Dashboard = () => {
    const [sidebarOpen, setSidebarOpen] = useState(false);

    return (
        <div className="flex h-screen bg-[#0B0B12] text-indigo-100 bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)]">
            <Sidebar isOpen={sidebarOpen} role="CUSTOMER" />

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
                        <h1 className="text-base font-semibold text-indigo-200">Dashboard</h1>
                    </div>
                </header>

                <main className="flex-1 px-6 py-6 md:px-8 md:py-8 overflow-auto">
                    <div className="max-w-7xl mx-auto">
                        <div className="motion-safe:animate-[fadeIn_.35s_ease-out]">
                            <h2 className="text-3xl md:text-4xl font-bold tracking-tight text-white">Welcome back</h2>
                            <p className="mt-1 text-sm text-indigo-300/80">
                                Here’s a quick snapshot of your account and activity.
                            </p>
                        </div>

                        <div className="mt-6 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6 motion-safe:animate-[riseIn_.35s_ease-out]">
                            <StatCard title="Total Users" value="1,024" icon={<span>👥</span>} tone="dark" />
                            <StatCard title="Revenue" value="$58,350" icon={<span>💰</span>} tone="dark" />
                            <StatCard title="Pending Tasks" value="12" icon={<span>📝</span>} tone="dark" />
                        </div>
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

export default Dashboard;
