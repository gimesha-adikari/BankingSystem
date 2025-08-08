import { Navigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import {JSX} from "react";

interface ProtectedRouteProps {
    children: JSX.Element;
    roles?: string[];
}

const ProtectedRoute = ({ children, roles }: ProtectedRouteProps) => {
    const { user, loading } = useAuth();

    if (loading) {
        return <div className="text-center mt-10">Checking authentication...</div>;
    }

    if (!user) {
        return <Navigate to="/login" />;
    }

    if (roles && !roles.includes(user.role)) {
        return <Navigate to="/unauthorized" />;
    }

    return children;
};

export default ProtectedRoute;
