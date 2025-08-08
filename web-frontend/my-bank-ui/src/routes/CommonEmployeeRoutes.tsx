import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";
import CustomerRegistration from "../pages/employee/CustomerRegistration.tsx";

export default function CommonEmployeeRoutes() {
    return (
        <ProtectedRoute roles={["ADMIN", "MANAGER","TELLER"]}>
            <Routes>
                <Route path="customers" element={<CustomerRegistration/>}/>
            </Routes>
        </ProtectedRoute>
    );
}
