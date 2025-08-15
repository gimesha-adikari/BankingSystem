import { useState, useEffect, useMemo } from "react";
import Sidebar from "@/components/Sidebar";
import InputField from "@/components/InputField";
import SelectField from "@/components/SelectField";
import { useAlert } from "@/contexts/use-alert";
import { useAuth } from "@/contexts/auth-context.ts";

type Gender = "MALE" | "FEMALE" | "OTHER";
type Status = "ACTIVE" | "INACTIVE" | "PENDING";

interface User {
    userId: string;
    firstName: string;
    lastName: string;
    gender: Gender;
    email: string;
    phone: string;
    address: string;
    dateOfBirth: string;
    status: Status;
}

interface Customer {
    customerId: string;
    firstName: string;
    lastName: string;
    gender: Gender;
    email: string;
    phone: string;
    address: string;
    dateOfBirth: string;
    status: Status;
    createdAt: string;
    updatedAt: string;
    userId: string;
}

function useDebounce(value: string, delay: number) {
    const [debouncedValue, setDebouncedValue] = useState(value);
    useEffect(() => {
        const handler = setTimeout(() => setDebouncedValue(value), delay);
        return () => clearTimeout(handler);
    }, [value, delay]);
    return debouncedValue;
}

const CustomerRegistration = () => {
    const { user, loading } = useAuth();
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [customerSearch, setCustomerSearch] = useState("");
    const [userSearch, setUserSearch] = useState("");

    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);

    const [formData, setFormData] = useState<User>({
        userId: "",
        firstName: "",
        lastName: "",
        gender: "MALE",
        email: "",
        phone: "",
        address: "",
        dateOfBirth: "",
        status: "ACTIVE",
    });

    const [customers, setCustomers] = useState<Customer[]>([]);
    const [users, setUsers] = useState<User[]>([]);

    const [loadingUsers, setLoadingUsers] = useState(false);
    const [loadingCustomers, setLoadingCustomers] = useState(false);

    const [errorUsers, setErrorUsers] = useState<string | null>(null);
    const [errorCustomers, setErrorCustomers] = useState<string | null>(null);

    const debouncedUserSearch = useDebounce(userSearch, 500);

    const { showAlert } = useAlert();
    const getAuthToken = () => localStorage.getItem("token");

    const getAuthHeaders = () => {
        const token = getAuthToken();
        return {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        };
    };

    const pretty = (val: string) => {
        if (!val) return "";
        return val.charAt(0).toUpperCase() + val.slice(1).toLowerCase();
    };

    if (loading) return <div className="min-h-screen grid place-items-center text-indigo-100">Loading…</div>;

    useEffect(() => {
        if (debouncedUserSearch.length === 0 || debouncedUserSearch.length >= 3) {
            setLoadingUsers(true);
            setErrorUsers(null);

            const query = debouncedUserSearch ? `?search=${encodeURIComponent(debouncedUserSearch)}` : "";

            fetch(`/api/v1/users/all${query}`, {
                method: "GET",
                headers: getAuthHeaders(),
                credentials: "include",
            })
                .then((res) => {
                    if (res.status === 401 || res.status === 403) throw new Error("Unauthorized. Please login again.");
                    if (!res.ok) throw new Error(`Failed to fetch users: ${res.status}`);
                    return res.json();
                })
                .then((data: User[]) => {
                    setUsers(data);
                    setLoadingUsers(false);
                })
                .catch((e) => {
                    setErrorUsers(e.message || "Failed to load users.");
                    setLoadingUsers(false);
                });
        } else {
            if (debouncedUserSearch.length > 0 && debouncedUserSearch.length < 3) setUsers([]);
        }
    }, [debouncedUserSearch]);

    useEffect(() => {
        setLoadingCustomers(true);
        setErrorCustomers(null);

        fetch("/api/v1/customers", {
            method: "GET",
            headers: getAuthHeaders(),
            credentials: "include",
        })
            .then((res) => {
                if (res.status === 401 || res.status === 403) throw new Error("Unauthorized. Please login again.");
                if (!res.ok) throw new Error(`Failed to fetch customers: ${res.status}`);
                return res.json();
            })
            .then((data: Customer[]) => {
                setCustomers(data);
                setLoadingCustomers(false);
            })
            .catch((e) => {
                setErrorCustomers(e.message || "Failed to load customers.");
                showAlert(e.message || "Failed to load customers.");
                setLoadingCustomers(false);
            });
    }, []);

    const filteredCustomers = useMemo(() => {
        const lowered = customerSearch.toLowerCase();
        return customers
            .filter((c) => c.firstName.toLowerCase().includes(lowered) || c.lastName.toLowerCase().includes(lowered) || c.email.toLowerCase().includes(lowered))
            .slice(0, 5);
    }, [customerSearch, customers]);

    const filteredUsers = useMemo(() => users.slice(0, 5), [users]);

    const onSelectUser = (u: User) => {
        setSelectedUser(u);
        setSelectedCustomer(null);
        setFormData({
            userId: u.userId,
            firstName: u.firstName,
            lastName: u.lastName,
            gender: u.gender,
            email: u.email,
            phone: u.phone,
            address: u.address,
            dateOfBirth: u.dateOfBirth,
            status: u.status,
        });
    };

    const onSelectCustomer = (c: Customer) => {
        setSelectedCustomer(c);
        setSelectedUser(null);
        setFormData({
            userId: c.userId,
            firstName: c.firstName,
            lastName: c.lastName,
            gender: c.gender,
            email: c.email,
            phone: c.phone,
            address: c.address,
            dateOfBirth: c.dateOfBirth,
            status: c.status,
        });
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({ ...prev, [name]: value } as unknown as User));
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        if (!formData.userId) {
            showAlert("User ID is required.");
            return;
        }
        if (!formData.firstName) {
            showAlert("First name is required.");
            return;
        }
        if (!formData.lastName) {
            showAlert("Last name is required.");
            return;
        }
        if (!formData.email) {
            showAlert("Email is required.");
            return;
        }
        if (!formData.phone) {
            showAlert("Phone is required.");
            return;
        }
        if (!formData.address) {
            showAlert("Address is required.");
            return;
        }
        if (!formData.dateOfBirth) {
            showAlert("Date of birth is required.");
            return;
        }
        if (!formData.status) {
            showAlert("Status is required.");
            return;
        }
        if (!formData.gender) {
            showAlert("Gender is required.");
            return;
        }

        try {
            const method = selectedCustomer ? "PUT" : "POST";
            const url = "/api/v1/customers";

            const response = await fetch(url, {
                method,
                headers: getAuthHeaders(),
                credentials: "include",
                body: JSON.stringify(formData),
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                showAlert(`Failed to save customer: ${errorData?.message || response.statusText || "Unknown error"}`);
                return;
            }

            showAlert("Customer saved successfully!", "success");

            setSelectedCustomer(null);
            setSelectedUser(null);
            setFormData({
                userId: "",
                firstName: "",
                lastName: "",
                gender: "MALE",
                email: "",
                phone: "",
                address: "",
                dateOfBirth: "",
                status: "ACTIVE",
            });

            setLoadingCustomers(true);
            setErrorCustomers(null);
            const customersRes = await fetch("/api/v1/customers", {
                method: "GET",
                headers: getAuthHeaders(),
                credentials: "include",
            });
            if (customersRes.ok) {
                const customersData = await customersRes.json();
                setCustomers(customersData);
            } else {
                setErrorCustomers("Failed to refresh customers list.");
            }
            setLoadingCustomers(false);
        } catch (error: any) {
            showAlert("An error occurred while saving: " + (error?.message || "Unknown error"));
        }
    };

    return (
        <div className="flex h-screen text-indigo-100 bg-[#0B0B12] bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)]">
            <Sidebar isOpen={sidebarOpen} role={user?.role ?? "CUSTOMER"} />

            <div className="flex-1 flex flex-col relative">
                <header className="md:hidden sticky top-0 z-40 bg-white/5 backdrop-blur ring-1 ring-white/10 py-3 px-4">
                    <div className="flex justify-between items-center">
                        <button
                            onClick={() => setSidebarOpen(!sidebarOpen)}
                            aria-label="Toggle sidebar"
                            aria-expanded={sidebarOpen}
                            className="h-9 w-9 grid place-items-center rounded-xl text-indigo-300 hover:text-white hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                        >
                            ☰
                        </button>
                        <h1 className="text-base font-semibold text-indigo-200">Customer Registration</h1>
                    </div>
                </header>

                <main className="flex-1 px-6 py-6 md:px-8 md:py-8 overflow-auto">
                    <button
                        onClick={() => window.history.back()}
                        aria-label="Go back"
                        title="Go back"
                        className="absolute top-4 right-4 rounded-full p-2 text-indigo-200 hover:text-white hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 md:h-6 md:w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>

                    <h1 className="text-3xl md:text-4xl font-bold tracking-tight text-white">Customer Registration</h1>

                    <div className="mt-6 grid grid-cols-1 lg:grid-cols-3 gap-8">
                        <div className="space-y-6 lg:col-span-1">
                            <div className="rounded-2xl ring-1 ring-white/10 bg-white/5 backdrop-blur p-4 md:p-5 shadow-sm">
                                <h2 className="text-lg font-semibold text-white mb-3">Customers</h2>
                                <input
                                    type="text"
                                    placeholder="Search customers by name or email"
                                    value={customerSearch}
                                    onChange={(e) => setCustomerSearch(e.target.value)}
                                    className="w-full mb-3 px-3 py-2 rounded-lg bg-white text-slate-900 placeholder:text-slate-400 ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none"
                                />

                                {loadingCustomers && <p className="text-center text-indigo-300/80 p-2" aria-live="polite">Loading customers…</p>}
                                {errorCustomers && <p className="text-center text-rose-400 p-2">{errorCustomers}</p>}

                                {!loadingCustomers && !errorCustomers && (
                                    <div className="overflow-hidden rounded-xl ring-1 ring-white/10">
                                        <table className="w-full text-sm">
                                            <thead className="bg-white/10 text-indigo-100 sticky top-0">
                                            <tr>
                                                <th className="px-3 py-2 text-left font-medium">Name</th>
                                                <th className="px-3 py-2 text-left font-medium">Email</th>
                                                <th className="px-3 py-2 text-left font-medium">Status</th>
                                            </tr>
                                            </thead>
                                            <tbody className="divide-y divide-white/10">
                                            {filteredCustomers.length === 0 && (
                                                <tr>
                                                    <td colSpan={3} className="px-3 py-3 text-center text-indigo-300/80 italic">No customers found.</td>
                                                </tr>
                                            )}
                                            {filteredCustomers.map((cust) => (
                                                <tr
                                                    key={cust.customerId}
                                                    onClick={() => onSelectCustomer(cust)}
                                                    className={[
                                                        "cursor-pointer transition",
                                                        selectedCustomer?.customerId === cust.customerId ? "bg-indigo-500/10" : "hover:bg-white/5",
                                                    ].join(" ")}
                                                >
                                                    <td className="px-3 py-2">{cust.firstName} {cust.lastName}</td>
                                                    <td className="px-3 py-2">{cust.email}</td>
                                                    <td className="px-3 py-2">{pretty(cust.status)}</td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>

                            <div className="rounded-2xl ring-1 ring-white/10 bg-white/5 backdrop-blur p-4 md:p-5 shadow-sm">
                                <h2 className="text-lg font-semibold text-white mb-3">Users</h2>
                                <input
                                    type="text"
                                    placeholder="Search users by name or email (min 3 chars)"
                                    value={userSearch}
                                    onChange={(e) => setUserSearch(e.target.value)}
                                    className="w-full mb-3 px-3 py-2 rounded-lg bg-white text-slate-900 placeholder:text-slate-400 ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none"
                                />

                                {loadingUsers && <p className="text-center text-indigo-300/80 p-2" aria-live="polite">Loading users…</p>}
                                {errorUsers && <p className="text-center text-rose-400 p-2">{errorUsers}</p>}

                                {!loadingUsers && !errorUsers && (
                                    <div className="overflow-hidden rounded-xl ring-1 ring-white/10">
                                        <table className="w-full text-sm">
                                            <thead className="bg-white/10 text-indigo-100 sticky top-0">
                                            <tr>
                                                <th className="px-3 py-2 text-left font-medium">Name</th>
                                                <th className="px-3 py-2 text-left font-medium">Email</th>
                                                <th className="px-3 py-2 text-left font-medium">Status</th>
                                            </tr>
                                            </thead>
                                            <tbody className="divide-y divide-white/10">
                                            {filteredUsers.length === 0 && (
                                                <tr>
                                                    <td colSpan={3} className="px-3 py-3 text-center text-indigo-300/80 italic">No users found.</td>
                                                </tr>
                                            )}
                                            {filteredUsers.map((u) => (
                                                <tr
                                                    key={u.userId}
                                                    onClick={() => onSelectUser(u)}
                                                    className={[
                                                        "cursor-pointer transition",
                                                        selectedUser?.userId === u.userId ? "bg-indigo-500/10" : "hover:bg-white/5",
                                                    ].join(" ")}
                                                >
                                                    <td className="px-3 py-2">{u.firstName} {u.lastName}</td>
                                                    <td className="px-3 py-2">{u.email}</td>
                                                    <td className="px-3 py-2">{pretty(u.status)}</td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>

                        <div className="lg:col-span-2">
                            <div className="rounded-2xl ring-1 ring-white/10 bg-white/5 backdrop-blur p-6 md:p-8 shadow-sm">
                                <h2 className="text-2xl font-semibold text-white mb-6">Customer / User Details</h2>

                                <form onSubmit={handleSubmit} className="space-y-6">
                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                        <InputField
                                            label="First Name"
                                            type="text"
                                            name="firstName"
                                            value={formData.firstName ?? ""}
                                            onChange={handleChange}
                                            placeholder="Enter first name"
                                            tone="light"
                                        />

                                        <InputField
                                            label="Last Name"
                                            type="text"
                                            name="lastName"
                                            value={formData.lastName ?? ""}
                                            onChange={handleChange}
                                            placeholder="Enter last name"
                                            tone="light"
                                        />

                                        <SelectField
                                            label="Gender"
                                            name="gender"
                                            value={formData.gender ?? ""}
                                            onChange={handleChange}
                                            placeholder="Select gender"
                                            options={[
                                                { value: "MALE", label: "Male" },
                                                { value: "FEMALE", label: "Female" },
                                                { value: "OTHER", label: "Other" },
                                            ]}
                                            tone="light"
                                        />

                                        <SelectField
                                            label="Status"
                                            name="status"
                                            value={formData.status ?? ""}
                                            onChange={handleChange}
                                            placeholder="Select status"
                                            options={[
                                                { value: "ACTIVE", label: "Active" },
                                                { value: "INACTIVE", label: "Inactive" },
                                                { value: "PENDING", label: "Pending" },
                                            ]}
                                            tone="light"
                                        />

                                        <InputField
                                            label="Email"
                                            type="email"
                                            name="email"
                                            value={formData.email ?? ""}
                                            onChange={handleChange}
                                            placeholder="Enter email"
                                            tone="light"
                                        />

                                        <InputField
                                            label="Phone"
                                            type="tel"
                                            name="phone"
                                            value={formData.phone ?? ""}
                                            onChange={handleChange}
                                            placeholder="Enter phone number"
                                            tone="light"
                                        />

                                        <div className="md:col-span-2">
                                            <InputField
                                                label="Address"
                                                type="text"
                                                name="address"
                                                value={formData.address ?? ""}
                                                onChange={handleChange}
                                                placeholder="Enter address"
                                                tone="light"
                                            />
                                        </div>

                                        <InputField
                                            label="Date of Birth"
                                            type="date"
                                            name="dateOfBirth"
                                            value={formData.dateOfBirth ?? ""}
                                            onChange={handleChange}
                                            tone="light"
                                        />
                                    </div>

                                    <div className="flex justify-end">
                                        <button
                                            type="submit"
                                            className="inline-flex items-center justify-center rounded-xl px-5 py-2.5 bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                                        >
                                            Save Customer
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

export default CustomerRegistration;
