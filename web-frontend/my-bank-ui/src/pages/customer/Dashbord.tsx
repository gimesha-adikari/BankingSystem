import { useState } from 'react';
import Sidebar from '../../components/Sidebar';
import StatCard from '../../components/StatCard';

const Dashboard = () => {
    const [sidebarOpen, setSidebarOpen] = useState(false);

    return (
        <div className="flex h-screen bg-gradient-to-br from-slate-300 to-slate-600 text-gray-900">
            <Sidebar isOpen={sidebarOpen} role="CUSTOMER"/>

            <div className="flex-1 flex flex-col">
                {/* Mobile Top Bar */}
                <header className="bg-slate-100 shadow-md py-4 px-6 flex justify-between items-center md:hidden">
                    <button
                        onClick={() => setSidebarOpen(!sidebarOpen)}
                        className="text-indigo-600 text-2xl focus:outline-none"
                    >
                        ☰
                    </button>
                    <h1 className="text-lg font-semibold text-gray-700">Dashboard</h1>
                </header>

                {/* Main Content */}
                <main className="flex-1 p-6">
                    <h2 className="text-3xl font-bold text-slate-800 mb-4">Welcome Back</h2>
                    <p className="text-slate-700 mb-6">
                        This is your dashboard. Start managing your application here.
                    </p>

                    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
                        <StatCard title="Total Users" value="1,024" />
                        <StatCard title="Revenue" value="$58,350" />
                        <StatCard title="Pending Tasks" value="12" />
                    </div>
                </main>
            </div>
        </div>
    );
};

export default Dashboard;