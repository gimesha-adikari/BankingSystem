import {useParams} from "react-router-dom";
import {AccountTransactionsList} from "../pages/accounts/AccountTransactionsList";
import ProtectedRoute from "../components/ProtectedRoute.tsx";

export default function AccountTransactionsRoute() {
    const {accountId} = useParams<{ accountId: string }>();
    if (!accountId) return null;
    return (
        <ProtectedRoute roles={["ADMIN", "MANAGER", "TELLER"]}>
            <AccountTransactionsList accountId={accountId}/>
        </ProtectedRoute>
    )
}
