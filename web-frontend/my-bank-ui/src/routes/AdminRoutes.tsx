import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";
import AdminHome from "../pages/admin/AdminHome.tsx";
import EmployeeManagement from "../pages/admin/EmployeeManagement.tsx";

export default function AdminRoutes() {
    return (
        <ProtectedRoute roles={["ADMIN"]}>
            <Routes>
                <Route path="home" element={<AdminHome/>}/>
                <Route path="employees" element={<EmployeeManagement/>}/>
            </Routes>
        </ProtectedRoute>
    );
}
