import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import PublicRoutes from "./routes/PublicRoutes";
import CustomerRoutes from "./routes/CustomerRoutes";
import AdminRoutes from "./routes/AdminRoutes";
import TellerRoutes from "./routes/TellerRoutes";
import ManagerRoutes from "./routes/ManagerRoutes";
import Alert from "./components/Alert";
import CommonEmployeeRoutes from "./routes/CommonEmployeeRoutes.tsx";

const App = () => {
    return (
        <Router>
            <Alert />
            <Routes>
                {/* Public routes */}
                <Route path="/*" element={<PublicRoutes />} />

                {/* Protected routes grouped by role */}
                <Route path="/employee/*" element={<CommonEmployeeRoutes />} />
                <Route path="/customer/*" element={<CustomerRoutes />} />
                <Route path="/admin/*" element={<AdminRoutes />} />
                <Route path="/teller/*" element={<TellerRoutes />} />
                <Route path="/manager/*" element={<ManagerRoutes />} />
            </Routes>
        </Router>
    );
};

export default App;
