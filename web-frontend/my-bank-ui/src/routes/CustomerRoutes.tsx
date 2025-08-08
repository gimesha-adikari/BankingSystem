import {Routes, Route} from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";
import Profile from "../pages/customer/ProfilePage";
import Dashboard from "../pages/customer/Dashbord";
import HomePage from "../pages/customer/HomePage";

export default function CustomerRoutes() {
    return (
        <ProtectedRoute roles={["CUSTOMER"]}>
            <Routes>
                <Route path="home" element={<HomePage/>}/>
                <Route path="dashboard" element={<Dashboard/>}/>
                <Route path="profile" element={<Profile/>}/>
            </Routes>
        </ProtectedRoute>
    );
}
