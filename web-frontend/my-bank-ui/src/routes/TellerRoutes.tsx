import {Routes, Route} from "react-router-dom";
import ProtectedRoute from "../components/ProtectedRoute";
import BankAccountCreation from "../pages/teller/BankAccountCreation.tsx";

export default function TellerRoutes() {
    return (
        <ProtectedRoute roles={["TELLER"]}>
            <Routes>
                <Route path="home" element={<BankAccountCreation/>}/>
            </Routes>
        </ProtectedRoute>
    );
}
