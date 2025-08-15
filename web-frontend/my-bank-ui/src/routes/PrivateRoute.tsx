import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/contexts/auth-context";
import type { Role } from "@/contexts/auth-context";

export default function PrivateRoute({ roles }: { roles?: Role[] }) {
    const { isAuthenticated, loading, hasRole } = useAuth();
    const location = useLocation();

    if (loading) {
        return (
            <div className="min-h-[60vh] grid place-items-center text-gray-500">
                Checking authentication…
            </div>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }

    if (roles && !hasRole(...roles)) {
        return <Navigate to="/unauthorized" replace />;
    }

    return <Outlet />;
}
