import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { AuthContext, type AuthContextType, type Role, type User } from "./auth-context";

const API_BASE = "";

export default function AuthProvider({ children }: { children: ReactNode }) {
    const [token, setToken] = useState<string | null>(() => localStorage.getItem("token"));
    const [user, setUser] = useState<User | null>(null);

    const [bootstrapped, setBootstrapped] = useState(false);

    const inflightRef = useRef<Promise<void> | null>(null);

    const isAuthenticated = !!token && !!user;

    const validateAndLoadUser = useCallback(async () => {
        if (!token) {
            setUser(null);
            setBootstrapped(true);
            return;
        }

        if (inflightRef.current) {
            await inflightRef.current;
            return;
        }

        inflightRef.current = (async () => {
            try {
                const res = await fetch(`${API_BASE}/api/v1/auth/validate-token`, {
                    headers: { Authorization: `Bearer ${token}` },
                });
                if (!res.ok) {
                    localStorage.removeItem("token");
                    setToken(null);
                    setUser(null);
                } else {
                    const data = (await res.json()) as { username: string; role: Role };
                    setUser({ username: data.username, role: data.role });
                }
            } catch {
                // keep token, clear user so we can retry later
                setUser(null);
            } finally {
                setBootstrapped(true);
                inflightRef.current = null;
            }
        })();

        await inflightRef.current;
    }, [token]);

    useEffect(() => {
        setBootstrapped(false);
        void validateAndLoadUser();
    }, [validateAndLoadUser]);

    const login = useCallback<AuthContextType["login"]>(
        async (newToken, knownUser) => {
            localStorage.setItem("token", newToken);
            setToken(newToken);
            // set user immediately to avoid a null window
            if (knownUser) {
                setUser(knownUser);
                setBootstrapped(true);
            } else {
                setBootstrapped(false);
                await validateAndLoadUser();
            }
        },
        [validateAndLoadUser]
    );

    const logout = useCallback<AuthContextType["logout"]>(
        async (opts) => {
            const revoke = opts?.revoke !== false;
            try {
                if (revoke && token) {
                    await fetch(`${API_BASE}/api/v1/auth/logout`, {
                        method: "POST",
                        headers: { Authorization: `Bearer ${token}` },
                    }).catch(() => {});
                }
            } finally {
                localStorage.removeItem("token");
                setToken(null);
                setUser(null);
                setBootstrapped(true);
            }
        },
        [token]
    );

    const getAuthHeaders = useCallback<AuthContextType["getAuthHeaders"]>(
        () => (token ? { Authorization: `Bearer ${token}` } : {}),
        [token]
    );

    const hasRole = useCallback<AuthContextType["hasRole"]>(
        (...roles) => !!user && roles.includes(user.role),
        [user]
    );

    const value = useMemo<AuthContextType>(
        () => ({
            user,
            token,
            loading: !bootstrapped,
            isAuthenticated,
            login,
            logout,
            getAuthHeaders,
            hasRole,
        }),
        [user, token, bootstrapped, isAuthenticated, login, logout, getAuthHeaders, hasRole]
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
