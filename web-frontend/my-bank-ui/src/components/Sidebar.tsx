import { NavLink, useNavigate, useLocation } from "react-router-dom";
import axios from "axios";
import { cn } from "@/utils/cn";
import React, { useEffect, useMemo, useRef } from "react";

type Role = "ADMIN" | "CUSTOMER" | "TELLER" | "MANAGER";

interface SidebarProps {
    isOpen: boolean;
    role: Role;
}

const menuItems: Record<Role, { title: string; icon: string; path: string }[]> = {
    ADMIN: [
        { title: "Home", icon: "🏠", path: "/admin/home" },
        { title: "User Management", icon: "👤", path: "/admin/users" },
        { title: "Reports", icon: "📊", path: "/admin/reports" },
        { title: "Settings", icon: "⚙️", path: "/admin/settings" },
        { title: "Customer Management", icon: "🧑‍💼", path: "/employee/customers" },
        { title: "Employee Management", icon: "🧑‍🤝‍🧑", path: "/admin/employees" },
    ],
    CUSTOMER: [
        { title: "Home", icon: "🏠", path: "/customer/home" },
        { title: "Dashboard", icon: "📊", path: "/customer/dashboard" },
        { title: "Profile", icon: "👤", path: "/customer/profile" },
    ],
    TELLER: [
        { title: "Home", icon: "🏠", path: "/teller/home" },
        { title: "Transactions", icon: "💸", path: "/teller/transactions" },
        { title: "Customer Support", icon: "📞", path: "/teller/support" },
        { title: "Customer Management", icon: "🧑‍💼", path: "/employee/customers" },
    ],
    MANAGER: [
        { title: "Home", icon: "🏠", path: "/manager/home" },
        { title: "Team Overview", icon: "👥", path: "/manager/team" },
        { title: "Reports", icon: "📈", path: "/manager/reports" },
    ],
};

const Sidebar = ({ isOpen, role }: SidebarProps) => {
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const items = menuItems[role];
    const activeRefs = useRef<Record<string, HTMLAnchorElement | null>>({});

    useEffect(() => {
        const el = activeRefs.current[pathname];
        if (el) el.scrollIntoView({ block: "nearest", inline: "nearest" });
    }, [pathname]);

    const brand = useMemo(
        () =>
            role === "ADMIN"
                ? { title: "MyBank Admin", subtitle: "Administration Panel" }
                : { title: "MyBank", subtitle: "Secure Banking Panel" },
        [role]
    );

    const handleLogout = async () => {
        const token = localStorage.getItem("token");
        try {
            if (token) {
                await axios.post(
                    "/api/v1/auth/logout",
                    {},
                    { headers: { Authorization: `Bearer ${token}` } }
                );
            }
        } catch {
        } finally {
            localStorage.removeItem("token");
            navigate("/login");
        }
    };

    return (
        <aside
            aria-label="Main sidebar"
            className={cn(
                "fixed md:relative inset-y-0 left-0 z-50 h-full w-72 md:w-64",
                "flex flex-col",
                "bg-gradient-to-b from-[#0B0B12] via-[#0B0B12] to-[#0E0E19]",
                "text-indigo-100 border-r border-white/10",
                "transform transition-transform duration-300 ease-in-out",
                isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0",
                "rounded-tr-3xl rounded-br-3xl shadow-[0_10px_40px_rgba(0,0,0,0.35)]"
            )}
        >
            <div className="sticky top-0 z-10 px-5 pt-6 pb-4 border-b border-white/10 bg-gradient-to-b from-white/5 to-transparent backdrop-blur">
                <div className="flex items-center gap-3">
                    <div className="h-10 w-10 rounded-2xl bg-indigo-600/20 ring-1 ring-indigo-500/30 flex items-center justify-center">
                        <span className="text-xl">🏦</span>
                    </div>
                    <div className="min-w-0">
                        <div className="font-semibold tracking-wide">{brand.title}</div>
                        <div className="text-[11px] text-indigo-300/80 truncate">{brand.subtitle}</div>
                    </div>
                </div>
            </div>

            <nav className="px-3 py-3 space-y-1 overflow-y-auto flex-1 scroll-pt-20">
                {items.map((item) => (
                    <NavLink
                        key={item.path}
                        to={item.path}
                        ref={(el) => {
                            if (el) activeRefs.current[item.path] = el;
                        }}
                        className={({ isActive }) =>
                            cn(
                                "group relative flex items-center gap-3 px-3 py-2.5 rounded-xl ring-1 transition",
                                "focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500",
                                isActive
                                    ? "bg-indigo-500/15 text-white ring-indigo-500"
                                    : "bg-white/0 text-indigo-200 ring-transparent hover:bg-white/5 hover:ring-white/10"
                            )
                        }
                    >
            <span
                aria-hidden
                className={cn(
                    "absolute left-0 top-1/2 -translate-y-1/2 h-6 w-1 rounded-r-md",
                    "bg-indigo-500 opacity-0 group-[.active]:opacity-100"
                )}
            />
                        <span className="text-[20px]">{item.icon}</span>
                        <span className="font-medium">{item.title}</span>
                        <span
                            aria-hidden
                            className={cn(
                                "ml-auto h-1.5 w-1.5 rounded-full bg-indigo-400/70",
                                "opacity-0 group-[.active]:opacity-100"
                            )}
                        />
                    </NavLink>
                ))}

                <div className="h-px my-3 bg-white/10 rounded-full" />

                <button
                    onClick={handleLogout}
                    className={cn(
                        "w-full flex items-center gap-3 px-3 py-2.5 rounded-xl",
                        "bg-rose-600/15 text-rose-300 ring-1 ring-rose-600/30",
                        "hover:bg-rose-600/25 hover:text-rose-200 transition",
                        "focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-500"
                    )}
                >
                    <span className="text-lg">🚪</span>
                    <span className="font-medium">Logout</span>
                </button>
            </nav>

            <div className="px-5 py-6 text-[11px] text-indigo-300/70 border-t border-white/10">
                &copy; 2025 MyBank Inc.
            </div>
        </aside>
    );
};

export default Sidebar;
