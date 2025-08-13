import { useState, useEffect } from 'react';
import Sidebar from '../../components/Sidebar'; // common sidebar
import HomeCard from '../../components/HomeCard';

const AdminHome = () => {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [username, setUsername] = useState('');

    useEffect(() => {
        const token = localStorage.getItem('token');
        setIsLoggedIn(!!token);

        if (token) {
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                setUsername(payload.sub);
            } catch {
                console.error('Invalid token');
            }
        }
    }, []);

    return (
        <div className="flex h-screen bg-gradient-to-b from-gray-600 to-gray-900">
            <Sidebar isOpen={sidebarOpen} role="ADMIN" />

            <div className="flex-1 flex flex-col">
                {/* Mobile Top Bar */}
                <header className="bg-gray-900 shadow-md py-4 px-6 flex justify-between items-center md:hidden">
                    <button
                        onClick={() => setSidebarOpen(!sidebarOpen)}
                        className="text-indigo-400 text-2xl focus:outline-none"
                        aria-label="Toggle sidebar"
                    >
                        ☰
                    </button>
                    <h1 className="text-lg font-semibold text-indigo-300">Admin Home</h1>
                </header>

                {/* Main Content */}
                <main className="flex-1 p-6 overflow-auto text-indigo-100">
                    {isLoggedIn ? (
                        <>
                            <h2 className="text-3xl font-bold mb-6">{`Welcome back, Admin ${username}!`}</h2>

                            <div className="grid gap-6 md:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3">
                                {[
                                    {
                                        title: 'User Management',
                                        description: 'Manage user accounts and roles.',
                                        link: '/admin/users',
                                        buttonText: 'Manage Users',
                                    },
                                    {
                                        title: 'Reports',
                                        description: 'View system reports and analytics.',
                                        link: '/admin/reports',
                                        buttonText: 'View Reports',
                                    },
                                    {
                                        title: 'Settings',
                                        description: 'Configure system settings and preferences.',
                                        link: '/admin/settings',
                                        buttonText: 'Settings',
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
                        <div className="text-center mt-20">
                            <h2 className="text-4xl font-bold mb-4 text-indigo-300">Welcome to MyBank Admin</h2>
                            <p className="text-indigo-400 mb-8 max-w-md mx-auto">
                                Admin portal to manage the banking system efficiently.
                            </p>
                            <a
                                href="/admin/login"
                                className="bg-indigo-600 text-white px-6 py-3 rounded hover:bg-indigo-700 transition"
                            >
                                Login
                            </a>
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
};

export default AdminHome;
