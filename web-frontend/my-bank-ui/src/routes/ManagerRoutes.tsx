import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";

export default function ManagerRoutes() {
    return (
        <ProtectedRoute roles={["MANAGER"]}>
            <Routes>
            </Routes>
        </ProtectedRoute>
    );
}
