import { Routes, Route } from "react-router-dom";
import PublicRoutes from "./routes/PublicRoutes";
import CustomerRoutes from "./routes/CustomerRoutes";
import AdminRoutes from "./routes/AdminRoutes";
import TellerRoutes from "./routes/TellerRoutes";
import ManagerRoutes from "./routes/ManagerRoutes";
import CommonEmployeeRoutes from "./routes/CommonEmployeeRoutes";
import BankAccountCreation from "./pages/teller/BankAccountCreation";
import AccountTransactionsRoute from "./routes/AppRoutes";
import PrivateRoute from "@/routes/PrivateRoute";
import Alert from "./components/Alert";

export default function App() {
    return (
        <>
            <Alert />
            <Routes>
                {/* public */}
                <Route path="/*" element={<PublicRoutes />} />

                {/* protected by role */}
                <Route element={<PrivateRoute roles={["CUSTOMER"]} />}>
                    <Route path="/customer/*" element={<CustomerRoutes />} />
                </Route>

                <Route element={<PrivateRoute roles={["ADMIN"]} />}>
                    <Route path="/admin/*" element={<AdminRoutes />} />
                </Route>

                <Route element={<PrivateRoute roles={["TELLER"]} />}>
                    <Route path="/teller/*" element={<TellerRoutes />} />
                    <Route path="/accounts/new" element={<BankAccountCreation />} />
                    <Route path="/accounts/:accountId/transactions" element={<AccountTransactionsRoute />} />
                </Route>

                <Route element={<PrivateRoute roles={["MANAGER"]} />}>
                    <Route path="/manager/*" element={<ManagerRoutes />} />
                </Route>

                {/* shared employee section */}
                <Route element={<PrivateRoute roles={["ADMIN","TELLER","MANAGER"]} />}>
                    <Route path="/employee/*" element={<CommonEmployeeRoutes />} />
                </Route>
            </Routes>
        </>
    );
}
