import {Routes, Route} from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";

export default function TellerRoutes() {
    return (
        <ProtectedRoute roles={["TELLER"]}>
            <Routes>
            </Routes>
        </ProtectedRoute>
    );
}
