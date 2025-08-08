import { useState, useEffect } from 'react';
import Sidebar from '../../components/Sidebar';
import HomeCard from '../../components/HomeCard';

const Home = () => {
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
            <Sidebar isOpen={sidebarOpen} />

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
                    <h1 className="text-lg font-semibold text-indigo-300">Home</h1>
                </header>

                {/* Main Content */}
                <main className="flex-1 p-6 overflow-auto text-indigo-100">
                    {isLoggedIn ? (
                        <>
                            <h2 className="text-3xl font-bold mb-6">{`Welcome back, ${username}!`}</h2>

                            <div className="grid gap-6 md:grid-cols-1 lg:grid-cols-2 xl:grid-cols-3">
                                {[
                                    {
                                        title: 'Accounts',
                                        description: 'View balances and manage your bank accounts.',
                                        link: '/accounts',
                                        buttonText: 'View Accounts',
                                    },
                                    {
                                        title: 'Transactions',
                                        description: 'Check your recent deposits, withdrawals, and transfers.',
                                        link: '/transactions',
                                        buttonText: 'View Transactions',
                                    },
                                    {
                                        title: 'Support',
                                        description: 'Need help? Contact our support team.',
                                        link: '/support',
                                        buttonText: 'Contact Support',
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
                            <h2 className="text-4xl font-bold mb-4 text-indigo-300">Welcome to MyBank</h2>
                            <p className="text-indigo-400 mb-8 max-w-md mx-auto">
                                Secure, reliable, and convenient banking solutions tailored for you.
                            </p>
                            <a
                                href="/register"
                                className="bg-indigo-600 text-white px-6 py-3 rounded hover:bg-indigo-700 transition"
                            >
                                Get Started
                            </a>
                        </div>
                    )}
                </main>
            </div>
        </div>
    );
};

export default Home;
