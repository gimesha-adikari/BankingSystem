import { createContext, useContext, useEffect, useState } from "react";

interface AuthContextType {
    user: { username: string; role: string } | null;
    loading: boolean;
    login: (token: string, username: string, role: string) => void;
    logout: () => void;
}

const AuthContext = createContext<AuthContextType>({
    user: null,
    loading: true,
    login: () => {},
    logout: () => {},
});

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
    const [user, setUser] = useState<{ username: string; role: string } | null>(null);
    const [loading, setLoading] = useState(true);
    const token = localStorage.getItem("token");

    const login = (token: string, username: string, role: string) => {
        localStorage.setItem("token", token);
        setUser({ username, role });
    };

    useEffect(() => {
        const validateToken = async () => {
            if (!token) {
                setUser(null);
                setLoading(false);
                return;
            }

            try {
                const response = await fetch("/api/v1/auth/validate-token", {
                    method: "GET",
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });

                if (!response.ok) {
                    setUser(null);
                } else {
                    const data = await response.json();
                    setUser({ username: data.username, role: data.role });
                }
            } catch (err) {
                console.error("Token validation failed", err);
                setUser(null);
            }
            setLoading(false);
        };

        validateToken();
    }, [token]);

    const logout = () => {
        localStorage.removeItem("token");
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);
