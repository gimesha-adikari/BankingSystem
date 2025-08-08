import {Routes, Route} from "react-router-dom";
import Login from "../pages/auth/Login";
import Unauthorized from "../pages/public/Unauthorized";
import ResetPasswordPage from "../pages/auth/ResetPasswordPage";
import Register from "../pages/auth/Register";
import GetsStarted from "../pages/public/GetsStarted";

export default function PublicRoutes() {
    return (
        <Routes>
            <Route path="" element={<GetsStarted/>}/>
            <Route path="login" element={<Login/>}/>
            <Route path="register" element={<Register/>}/>
            <Route path="reset-password" element={<ResetPasswordPage/>}/>
            <Route path="unauthorized" element={<Unauthorized/>}/>
        </Routes>
    );
}
