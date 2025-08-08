import { Link, useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';

const Sidebar = ({ isOpen }: { isOpen: boolean }) => {
    const location = useLocation();
    const navigate = useNavigate();

    const linkClasses = (path: string) =>
        `flex items-center gap-3 px-5 py-3 rounded-xl border border-transparent transition-all duration-200 ease-in-out
         ${
            location.pathname === path
                ? 'bg-indigo-700 text-white shadow-inner border-indigo-800'
                : 'bg-gray-50 hover:bg-indigo-100 hover:border-indigo-400 text-gray-700 shadow-sm hover:shadow-lg'
        }`;

    const handleLogout = async () => {
        try {
            const token = localStorage.getItem('token');
            if (!token) return;

            await axios.post(
                '/api/v1/auth/logout',
                {},
                {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            localStorage.removeItem('token');
            navigate('/login')
        } catch (error) {
            console.error('Logout failed:', error);
            alert('Logout failed. Please try again.');
        }
    };

    return (
        <aside
            className={`w-64 bg-gradient-to-b from-gray-100 via-gray-200 to-gray-300 text-gray-900 border-r border-gray-300 py-7 px-6
                        absolute inset-y-0 left-0 transform rounded-tr-3xl rounded-br-3xl drop-shadow-2xl
                        ${isOpen ? 'translate-x-0' : '-translate-x-full'} md:relative md:translate-x-0
                        transition-transform duration-300 ease-in-out z-50`}
        >
            <div className="mb-12 text-center">
                <h2 className="text-3xl font-extrabold text-indigo-800 tracking-widest mb-2 select-none">🏦 MyBank</h2>
                <p className="text-xs text-gray-500 uppercase tracking-wide select-none">Secure Banking Panel</p>
            </div>

            <nav className="flex flex-col gap-5">
                <Link to="/customer/home" className={linkClasses('/')}>
                    <span className="text-lg">🏠</span>
                    <span className="font-semibold">Home</span>
                </Link>
                <Link to="/customer/dashboard" className={linkClasses('/dashboard')}>
                    <span className="text-lg">📊</span>
                    <span className="font-semibold">Dashboard</span>
                </Link>
                <Link to="/customer/profile" className={linkClasses('/profile')}>
                    <span className="text-lg">👤</span>
                    <span className="font-semibold">Profile</span>
                </Link>

                {/* Logout Button */}
                <button
                    onClick={handleLogout}
                    className="mt-4 flex items-center gap-3 px-5 py-3 rounded-xl bg-red-100 text-red-600 hover:bg-red-200 transition-all duration-200"
                >
                    <span className="text-lg">🚪</span>
                    <span className="font-semibold">Logout</span>
                </button>
            </nav>

            <div className="mt-auto text-center text-xs text-gray-500 border-t border-gray-300 pt-6 select-none">
                &copy; 2025 MyBank Inc.
            </div>
        </aside>
    );
};

export default Sidebar;
