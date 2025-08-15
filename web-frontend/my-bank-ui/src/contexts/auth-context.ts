import { createContext, useContext } from "react";

export type Role = "ADMIN" | "CUSTOMER" | "TELLER" | "MANAGER";

export interface User {
    username: string;
    role: Role;
}

export interface AuthContextType {
    user: User | null;
    token: string | null;
    loading: boolean;
    bootstrapped: boolean;
    isAuthenticated: boolean;
    login: (token: string, user?: User) => Promise<void>;
    logout: (opts?: { revoke?: boolean }) => Promise<void>;
    getAuthHeaders: () => Record<string, string>;
    hasRole: (...roles: Role[]) => boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function useAuth(): AuthContextType {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
    return ctx;
}
