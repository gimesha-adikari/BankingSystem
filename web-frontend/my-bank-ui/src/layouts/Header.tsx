import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

const Header = () => {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem('token');
        setIsLoggedIn(!!token);
    }, []);

    const handleLogout = () => {
        localStorage.removeItem('token');
        setIsLoggedIn(false);
        navigate('/login');
    };

    return (
        <header className="bg-gray-900 text-white px-6 py-4 flex justify-between items-center shadow-md">
            <h1 className="text-2xl font-bold tracking-wide text-indigo-300">MyBank</h1>
            <nav className="space-x-4 text-sm font-medium">
                <Link to="/" className="hover:text-indigo-300 transition">
                    Home
                </Link>
                <Link to="/dashboard" className="hover:text-indigo-300 transition">
                    Dashboard
                </Link>
                {isLoggedIn ? (
                    <>
                        <Link to="/profile" className="hover:text-indigo-300 transition">
                            Profile
                        </Link>
                        <button
                            onClick={handleLogout}
                            className="hover:text-red-400 transition focus:outline-none"
                        >
                            Logout
                        </button>
                    </>
                ) : (
                    <>
                        <Link to="/login" className="hover:text-indigo-300 transition">
                            Login
                        </Link>
                        <Link to="/register" className="hover:text-indigo-300 transition">
                            Register
                        </Link>
                    </>
                )}
            </nav>
        </header>
    );
};

export default Header;
