import { useEffect, useMemo, useState } from "react";
import Sidebar from "../../components/Sidebar";
import InputField from "../../components/InputField";
import SelectField from "../../components/SelectField";
import { useAlert } from "@/contexts/use-alert";
import { useAuth } from "@/contexts/auth-context";
import { useNavigate } from "react-router-dom";
import api from "../../api/axios";

type AccountType = "SAVINGS" | "CHECKING" | "FIXED_DEPOSIT";

interface Customer {
    customerId: string;
    firstName: string;
    lastName: string;
    email: string;
    userId: string;
}

interface Branch {
    branchId: string;
    branchName: string;
}

const MIN_DEPOSIT: Record<AccountType, number> = {
    SAVINGS: 1000,
    CHECKING: 0,
    FIXED_DEPOSIT: 5000,
};

const BankAccountCreation = () => {
    const { user, loading } = useAuth();
    const role = user?.role ?? "GUEST";
    const isTeller = role === "TELLER";
    const isCustomer = role === "CUSTOMER";

    const { showAlert } = useAlert();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const navigate = useNavigate();

    const [customerSearch, setCustomerSearch] = useState("");
    const [customers, setCustomers] = useState<Customer[]>([]);
    const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);

    const [branches, setBranches] = useState<Branch[]>([]);
    const [branchId, setBranchId] = useState<string | null>(null);

    const [formData, setFormData] = useState<{ accountType: AccountType; initialDeposit: string }>({
        accountType: "SAVINGS",
        initialDeposit: "",
    });

    const [errors, setErrors] = useState<Record<string, string>>({});
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (!isTeller) return;
        const ac = new AbortController();
        api
            .get("/api/v1/customers", { signal: ac.signal })
            .then((res) => setCustomers(res.data))
            .catch(() => showAlert("Failed to load customers"));
        return () => ac.abort();
    }, [isTeller, showAlert]);

    useEffect(() => {
        const ac = new AbortController();
        api
            .get("/api/v1/branches", { signal: ac.signal })
            .then((res) => setBranches(res.data))
            .catch(() => showAlert("Failed to load branches"));
        return () => ac.abort();
    }, [showAlert]);

    const filteredCustomers = useMemo(() => {
        if (!isTeller) return [];
        const lower = customerSearch.toLowerCase();
        return customers.filter(
            (c) =>
                c.firstName.toLowerCase().includes(lower) ||
                c.lastName.toLowerCase().includes(lower) ||
                c.email.toLowerCase().includes(lower)
        );
    }, [customerSearch, customers, isTeller]);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setErrors((prev) => ({ ...prev, [name]: "" }));
        if (name === "accountType") {
            setFormData((p) => ({ ...p, accountType: value as AccountType }));
        } else if (name === "initialDeposit") {
            setFormData((p) => ({ ...p, initialDeposit: value }));
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (submitting) return;
        setErrors({});

        if (!isTeller && !isCustomer) {
            showAlert("Your role is not permitted to open accounts.");
            return;
        }
        if (isTeller && !selectedCustomer) {
            setErrors((p) => ({ ...p, customerId: "Please select a customer." }));
            showAlert("Select a customer before creating an account.");
            return;
        }
        if (!branchId) {
            setErrors((p) => ({ ...p, branchId: "Please select a branch." }));
            showAlert("Please select a branch.");
            return;
        }

        const depositNumber = Number(formData.initialDeposit);
        if (!Number.isFinite(depositNumber) || depositNumber < 0) {
            setErrors((p) => ({ ...p, initialDeposit: "Initial deposit must be a valid non-negative number." }));
            showAlert("Initial deposit must be a valid non-negative number.");
            return;
        }
        const min = MIN_DEPOSIT[formData.accountType];
        if (depositNumber < min) {
            const msg = `Minimum initial deposit for ${formData.accountType.replace("_", " ")} is ${min.toFixed(2)}.`;
            setErrors((p) => ({ ...p, initialDeposit: msg }));
            showAlert(msg);
            return;
        }

        const payload = {
            accountType: formData.accountType,
            initialDeposit: depositNumber,
            branchId: branchId,
        };

        try {
            setSubmitting(true);
            const url = isTeller ? `/api/v1/accounts/${selectedCustomer!.userId}` : `/api/v1/accounts`;
            const res = await api.post(url, payload);
            showAlert("Account created successfully", "success");
            const accountId = res.data?.accountId;
            if (accountId) navigate(`/accounts/${accountId}/transactions`);

            setFormData({ accountType: "SAVINGS", initialDeposit: "" });
            setBranchId(null);
        } catch (err: any) {
            if (err.violations) {
                setErrors(err.violations);
                const first = Object.values(err.violations)[0];
                showAlert((first as string) || err.message || "Error creating account");
            } else {
                showAlert(err.message || "Error creating account");
            }
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) return <div className="min-h-screen grid place-items-center text-indigo-100 bg-[#0B0B12]">Loading…</div>;

    return (
        <div className="flex h-screen text-indigo-100 bg-[#0B0B12] bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)]">
            <Sidebar isOpen={sidebarOpen} role={role} />
            <div className="flex-1 flex flex-col relative">
                <header className="md:hidden sticky top-0 z-40 bg-white/5 backdrop-blur ring-1 ring-white/10 py-3 px-4">
                    <div className="flex justify-between items-center">
                        <button
                            onClick={() => setSidebarOpen(!sidebarOpen)}
                            className="h-9 w-9 grid place-items-center rounded-xl text-indigo-300 hover:text-white hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                            aria-label="Toggle sidebar"
                            aria-expanded={sidebarOpen}
                        >
                            ☰
                        </button>
                        <h1 className="text-base font-semibold text-indigo-200">Bank Account Creation</h1>
                    </div>
                </header>

                <main className="flex-1 px-6 py-6 md:px-8 md:py-8 overflow-auto">
                    <button
                        onClick={() => window.history.back()}
                        className="absolute top-4 right-4 rounded-full p-2 text-indigo-200 hover:text-white hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                        aria-label="Close"
                        title="Close"
                    >
                        ✕
                    </button>

                    <h1 className="text-3xl md:text-4xl font-bold tracking-tight text-white">Bank Account Creation</h1>

                    <div className={`mt-6 grid gap-8 ${isTeller ? "lg:grid-cols-3" : "grid-cols-1"}`}>
                        {isTeller && (
                            <div className="lg:col-span-1 space-y-6">
                                <div className="rounded-2xl ring-1 ring-white/10 bg-white/5 backdrop-blur p-4 md:p-5 shadow-sm">
                                    <h2 className="text-lg font-semibold text-white mb-3">Customers</h2>
                                    <input
                                        type="text"
                                        placeholder="Search customers…"
                                        value={customerSearch}
                                        onChange={(e) => setCustomerSearch(e.target.value)}
                                        className="w-full mb-3 px-3 py-2 rounded-lg bg-white text-slate-900 placeholder:text-slate-400 ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none"
                                    />
                                    {errors.customerId && <p className="text-rose-400 text-sm mb-2">{errors.customerId}</p>}

                                    <div className="overflow-hidden rounded-xl ring-1 ring-white/10">
                                        <table className="w-full text-sm">
                                            <thead className="bg-white/10 text-indigo-100 sticky top-0">
                                            <tr>
                                                <th className="px-3 py-2 text-left font-medium">Name</th>
                                                <th className="px-3 py-2 text-left font-medium">Email</th>
                                            </tr>
                                            </thead>
                                            <tbody className="divide-y divide-white/10">
                                            {filteredCustomers.length === 0 && (
                                                <tr>
                                                    <td colSpan={2} className="px-3 py-3 text-center text-indigo-300/80 italic">
                                                        No customers found.
                                                    </td>
                                                </tr>
                                            )}
                                            {filteredCustomers.map((cust) => (
                                                <tr
                                                    key={cust.customerId}
                                                    onClick={() => setSelectedCustomer(cust)}
                                                    className={[
                                                        "cursor-pointer transition",
                                                        selectedCustomer?.customerId === cust.customerId ? "bg-indigo-500/10" : "hover:bg-white/5",
                                                    ].join(" ")}
                                                >
                                                    <td className="px-3 py-2">{cust.firstName} {cust.lastName}</td>
                                                    <td className="px-3 py-2">{cust.email}</td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        )}

                        <div className={`${isTeller ? "lg:col-span-2" : "col-span-1"}`}>
                            <div className="rounded-2xl ring-1 ring-white/10 bg-white/5 backdrop-blur p-6 md:p-8 shadow-sm">
                                <h2 className="text-2xl font-semibold text-white mb-6">Account Details</h2>

                                <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-6">
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
                                        required
                                        tone="light"
                                    />
                                    {errors.accountType && <p className="text-rose-400 text-sm -mt-4">{errors.accountType}</p>}

                                    <InputField
                                        label={`Initial Deposit (min ${MIN_DEPOSIT[formData.accountType].toFixed(2)})`}
                                        type="number"
                                        name="initialDeposit"
                                        value={formData.initialDeposit}
                                        onChange={handleChange}
                                        placeholder="0.00"
                                        step="0.01"
                                        min="0"
                                        required
                                        tone="light"
                                    />
                                    {errors.initialDeposit && <p className="text-rose-400 text-sm -mt-4">{errors.initialDeposit}</p>}

                                    <SelectField
                                        label="Branch"
                                        name="branchId"
                                        value={branchId ?? ""}
                                        onChange={(e) => setBranchId(e.target.value || null)}
                                        options={branches.map((b) => ({ value: b.branchId, label: b.branchName }))}
                                        required
                                        tone="light"
                                    />
                                    {errors.branchId && <p className="text-rose-400 text-sm -mt-4">{errors.branchId}</p>}

                                    <div className="md:col-span-2 mt-2">
                                        <button
                                            type="submit"
                                            disabled={submitting || (isTeller && !selectedCustomer)}
                                            className={[
                                                "inline-flex items-center justify-center rounded-xl px-6 py-2.5 text-white transition shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500",
                                                submitting || (isTeller && !selectedCustomer)
                                                    ? "bg-indigo-400 cursor-not-allowed"
                                                    : "bg-indigo-600 hover:bg-indigo-700",
                                            ].join(" ")}
                                        >
                                            {submitting ? "Creating..." : "Create Account"}
                                        </button>
                                    </div>
                                </form>
                            </div>
                        </div>
                    </div>
                </main>
            </div>

            <style>
                {`
          @keyframes riseIn {
            from { opacity: 0; transform: translateY(8px) scale(0.99); }
            to   { opacity: 1; transform: translateY(0) scale(1); }
          }
        `}
            </style>
        </div>
    );
};

export default BankAccountCreation;
