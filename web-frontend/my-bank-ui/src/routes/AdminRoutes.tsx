import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";

export default function AdminRoutes() {
    return (
        <ProtectedRoute roles={["ADMIN"]}>
            <Routes>
            </Routes>
        </ProtectedRoute>
    );
}
