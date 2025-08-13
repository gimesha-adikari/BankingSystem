import { useState, useEffect, useMemo } from "react";
import Sidebar from "../../components/Sidebar";
import InputField from "../../components/InputField";
import SelectField from "../../components/SelectField";
import { useAlert } from "../../contexts/AlertContext";
import { useAuth } from "../../contexts/AuthContext";

type AccountType = "SAVINGS" | "CHECKING" | "FIXED_DEPOSIT";

interface Customer {
    customerId: string;
    firstName: string;
    lastName: string;
    email: string;
    userId: string;
}

interface Branch {
    branchId: number;
    branchName: string;
}

const BankAccountCreation = () => {
    const { user, loading } = useAuth();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [customerSearch, setCustomerSearch] = useState("");
    const [customers, setCustomers] = useState<Customer[]>([]);
    const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
    const [branches, setBranches] = useState<Branch[]>([]);
    const [branchId, setBranchId] = useState<number | null>(null);

    const [formData, setFormData] = useState({
        accountNumber: "",
        accountType: "SAVINGS" as AccountType,
        balance: 0,
    });

    const { showAlert } = useAlert();

    const getAuthHeaders = () => {
        const token = localStorage.getItem("token");
        return {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        };
    };

    useEffect(() => {
        fetch("/api/v1/customers", { headers: getAuthHeaders() })
            .then((res) => res.json())
            .then((data) => setCustomers(data))
            .catch(() => showAlert("Failed to load customers"));
    }, []);

    useEffect(() => {
        fetch("/api/v1/branches", { headers: getAuthHeaders() })
            .then((res) => res.json())
            .then((data) => setBranches(data))
            .catch(() => showAlert("Failed to load branches"));
    }, []);

    const filteredCustomers = useMemo(() => {
        const lower = customerSearch.toLowerCase();
        return customers.filter(
            (c) =>
                c.firstName.toLowerCase().includes(lower) ||
                c.lastName.toLowerCase().includes(lower) ||
                c.email.toLowerCase().includes(lower)
        );
    }, [customerSearch, customers]);

    const onSelectCustomer = (cust: Customer) => {
        setSelectedCustomer(cust);
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: name === "balance" ? parseFloat(value) : value,
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!selectedCustomer) {
            showAlert("Select a customer before creating an account.");
            return;
        }

        if (branchId === null) {
            showAlert("Please select a branch.");
            return;
        }

        const payload = {
            accountNumber: formData.accountNumber,
            accountType: formData.accountType,
            initialDeposit: formData.balance,
            branchId: branchId,
        };

        try {
            const res = await fetch(`/api/v1/accounts/${selectedCustomer.userId}`, {
                method: "POST",
                headers: getAuthHeaders(),
                body: JSON.stringify(payload),
            });
            if (!res.ok) throw new Error("Failed to create account");
            showAlert("Account created successfully", "success");

            // Reset form
            setFormData({
                accountNumber: "",
                accountType: "SAVINGS",
                balance: 0,
            });
            setSelectedCustomer(null);
            setBranchId(null);
        } catch (err: any) {
            showAlert(err.message || "Error creating account");
        }
    };

    if (loading) {
        return <div>Loading...</div>;
    }

    return (
        <div className="flex h-screen bg-gradient-to-b from-gray-600 to-gray-900">
            <Sidebar isOpen={sidebarOpen} role={user?.role ?? "GUEST"} />
            <div className="flex-1 flex flex-col relative">
                {/* Mobile Top Bar */}
                <header className="bg-gray-900 shadow-md py-4 px-6 flex justify-between items-center md:hidden">
                    <button
                        onClick={() => setSidebarOpen(!sidebarOpen)}
                        className="text-indigo-400 text-2xl focus:outline-none"
                    >
                        ☰
                    </button>
                    <h1 className="text-lg font-semibold text-indigo-300">Bank Account Creation</h1>
                </header>

                <main className="flex-1 p-6 overflow-auto text-indigo-100">
                    <button
                        onClick={() => window.history.back()}
                        className="absolute top-4 right-4 text-gray-300 hover:text-white rounded-full p-2"
                    >
                        ✕
                    </button>

                    <h1 className="text-3xl font-bold mb-8 text-indigo-200">Bank Account Creation</h1>

                    <div className="grid grid-cols-3 gap-8">
                        {/* Left: Customer list */}
                        <div className="col-span-1 space-y-8">
                            <div>
                                <h2 className="text-xl font-semibold mb-3 text-indigo-300">Customers</h2>
                                <input
                                    type="text"
                                    placeholder="Search customers..."
                                    value={customerSearch}
                                    onChange={(e) => setCustomerSearch(e.target.value)}
                                    className="w-full mb-2 px-3 py-2 border rounded bg-gray-700 text-indigo-100 border-gray-600"
                                />
                                <table className="w-full border-collapse border border-gray-600 text-sm">
                                    <thead>
                                    <tr className="bg-indigo-900">
                                        <th className="border border-gray-600 px-2 py-1">Name</th>
                                        <th className="border border-gray-600 px-2 py-1">Email</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {filteredCustomers.map((cust) => (
                                        <tr
                                            key={cust.customerId}
                                            onClick={() => onSelectCustomer(cust)}
                                            className={`cursor-pointer hover:bg-indigo-700 ${
                                                selectedCustomer?.customerId === cust.customerId ? "bg-indigo-800 font-semibold" : ""
                                            }`}
                                        >
                                            <td className="border border-gray-600 px-2 py-1">
                                                {cust.firstName} {cust.lastName}
                                            </td>
                                            <td className="border border-gray-600 px-2 py-1">{cust.email}</td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        {/* Right: Form */}
                        <div className="col-span-2 bg-gray-800 p-8 rounded-lg shadow-lg">
                            <h2 className="text-2xl font-semibold mb-6">Account Details</h2>
                            <form onSubmit={handleSubmit} className="grid grid-cols-2 gap-6">
                                <InputField
                                    label="Account Number"
                                    type="text"
                                    name="accountNumber"
                                    value={formData.accountNumber}
                                    onChange={handleChange}
                                    placeholder="Enter account number"
                                />
                                <SelectField
                                    label="Account Type"
                                    name="accountType"
                                    value={formData.accountType}
                                    onChange={handleChange}
                                    options={[
                                        { value: "SAVINGS", label: "Savings" },
                                        { value: "CHECKING", label: "Checking" },
                                        { value: "FIXED_DEPOSIT", label: "Fixed Deposit" },
                                    ]}
                                />
                                <InputField
                                    label="Initial Deposit"
                                    type="number"
                                    name="balance"
                                    value={formData.balance}
                                    onChange={handleChange}
                                    placeholder="0.00"
                                />
                                <SelectField
                                    label="Branch"
                                    name="branchId"
                                    value={branchId ?? ""}
                                    onChange={(e) => setBranchId(parseInt(e.target.value, 10))}
                                    options={branches.map((branch) => ({
                                        value: branch.branchId.toString(),
                                        label: branch.branchName,
                                    }))}
                                />
                                <div className="col-span-2 mt-4">
                                    <button
                                        type="submit"
                                        className="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700"
                                    >
                                        Create Account
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </main>
            </div>
        </div>
    );
};

export default BankAccountCreation;
