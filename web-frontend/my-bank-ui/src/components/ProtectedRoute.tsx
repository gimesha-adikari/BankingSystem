import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "@/contexts/auth-context";
import type { JSX } from "react";
import type { Role } from "@/contexts/auth-context";

interface ProtectedRouteProps {
    children: JSX.Element;
    roles?: Role[];
}

export default function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
    const { user, loading, hasRole } = useAuth();
    const location = useLocation();

    if (loading) {
        return <div className="text-center mt-10">Checking authentication...</div>;
    }
    if (!user) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }
    if (roles && !hasRole(...roles)) {
        return <Navigate to="/unauthorized" replace />;
    }
    return children;
}
