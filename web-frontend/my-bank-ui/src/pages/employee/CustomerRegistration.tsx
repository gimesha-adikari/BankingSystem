import { useState, useEffect, useMemo } from "react";
import Sidebar from "../../components/Sidebar"; // common sidebar with role prop
import InputField from "../../components/InputField";
import SelectField from "../../components/SelectField";
import { useAlert } from "../../contexts/AlertContext";
import {useAuth} from "../../contexts/AuthContext.tsx";

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
    const getRole = () => localStorage.getItem("role");

    const getAuthHeaders = () => {
        const token = getAuthToken();
        return {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        };
    };

    // Helper to display friendly labels for enums in the UI
    const pretty = (val: string) => {
        if (!val) return "";
        return val.charAt(0).toUpperCase() + val.slice(1).toLowerCase();
    };
    if (loading) {
        return <div>Loading...</div>;
    }


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
                    if (res.status === 401 || res.status === 403) {
                        throw new Error("Unauthorized. Please login again.");
                    }
                    if (!res.ok) {
                        throw new Error(`Failed to fetch users: ${res.status}`);
                    }
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
            if (debouncedUserSearch.length > 0 && debouncedUserSearch.length < 3) {
                setUsers([]);
            }
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
                if (res.status === 401 || res.status === 403) {
                    throw new Error("Unauthorized. Please login again.");
                }
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
            .filter((c) => {
                return (
                    c.firstName.toLowerCase().includes(lowered) ||
                    c.lastName.toLowerCase().includes(lowered) ||
                    c.email.toLowerCase().includes(lowered)
                );
            })
            .slice(0, 5);
    }, [customerSearch, customers]);

    const filteredUsers = useMemo(() => users.slice(0, 5), [users]);

    const onSelectUser = (user: User) => {
        setSelectedUser(user);
        setSelectedCustomer(null);
        setFormData({
            userId: user.userId,
            firstName: user.firstName,
            lastName: user.lastName,
            gender: user.gender,
            email: user.email,
            phone: user.phone,
            address: user.address,
            dateOfBirth: user.dateOfBirth,
            status: user.status,
        });
    };

    const onSelectCustomer = (customer: Customer) => {
        setSelectedCustomer(customer);
        setSelectedUser(null);
        setFormData({
            userId: customer.userId,
            firstName: customer.firstName,
            lastName: customer.lastName,
            gender: customer.gender,
            email: customer.email,
            phone: customer.phone,
            address: customer.address,
            dateOfBirth: customer.dateOfBirth,
            status: customer.status,
        });
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        } as unknown as User));
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

            // Reset selections and form
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

            // Refresh customers list
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
        <div className="flex h-screen bg-gradient-to-b from-gray-600 to-gray-900">
            {/* Sidebar with role prop if your Sidebar supports it */}
            <Sidebar isOpen={sidebarOpen} role={user?.role ?? "GUEST"} />

            <div className="flex-1 flex flex-col relative">
                {/* Mobile Top Bar */}
                <header className="bg-gray-900 shadow-md py-4 px-6 flex justify-between items-center md:hidden">
                    <button
                        onClick={() => setSidebarOpen(!sidebarOpen)}
                        className="text-indigo-400 text-2xl focus:outline-none"
                        aria-label="Toggle sidebar"
                    >
                        ☰
                    </button>
                    <h1 className="text-lg font-semibold text-indigo-300">Customer Registration</h1>
                </header>

                {/* Main Content */}
                <main className="flex-1 p-6 overflow-auto text-indigo-100">
                    {/* Close button */}
                    <button
                        onClick={() => window.history.back()}
                        aria-label="Go back"
                        className="absolute top-4 right-4 text-gray-300 hover:text-white rounded-full p-2 focus:outline-none focus:ring-2 focus:ring-indigo-400"
                        title="Go back"
                    >
                        <svg
                            xmlns="http://www.w3.org/2000/svg"
                            className="h-6 w-6"
                            fill="none"
                            viewBox="0 0 24 24"
                            stroke="currentColor"
                            strokeWidth={2}
                        >
                            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>

                    <h1 className="text-3xl font-bold mb-8 text-indigo-200">Customer Registration</h1>

                    <div className="grid grid-cols-3 gap-8">
                        {/* Left panel: Customers and Users */}
                        <div className="col-span-1 space-y-8">
                            {/* Customers */}
                            <div>
                                <h2 className="text-xl font-semibold mb-3 text-indigo-300">Customers</h2>
                                <input
                                    type="text"
                                    placeholder="Search customers by name or email"
                                    value={customerSearch}
                                    onChange={(e) => setCustomerSearch(e.target.value)}
                                    className="w-full mb-2 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-gray-700 text-indigo-100 border-gray-600"
                                />

                                {loadingCustomers && <p className="text-center text-gray-400 p-2">Loading customers...</p>}
                                {errorCustomers && <p className="text-center text-red-500 p-2">{errorCustomers}</p>}

                                {!loadingCustomers && !errorCustomers && (
                                    <table className="w-full border-collapse border border-gray-600 text-sm text-indigo-100">
                                        <thead>
                                        <tr className="bg-indigo-900">
                                            <th className="border border-gray-600 px-2 py-1 text-left">Name</th>
                                            <th className="border border-gray-600 px-2 py-1 text-left">Email</th>
                                            <th className="border border-gray-600 px-2 py-1 text-left">Status</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {filteredCustomers.length === 0 && (
                                            <tr>
                                                <td colSpan={3} className="text-center p-2 text-gray-500">
                                                    No customers found.
                                                </td>
                                            </tr>
                                        )}
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
                                                <td className="border border-gray-600 px-2 py-1">{pretty(cust.status)}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>

                            {/* Users */}
                            <div>
                                <h2 className="text-xl font-semibold mb-3 text-indigo-300">Users</h2>
                                <input
                                    type="text"
                                    placeholder="Search users by name or email (min 3 chars)"
                                    value={userSearch}
                                    onChange={(e) => setUserSearch(e.target.value)}
                                    className="w-full mb-2 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-gray-700 text-indigo-100 border-gray-600"
                                />

                                {loadingUsers && <p className="text-center text-gray-400 p-2">Loading users...</p>}
                                {errorUsers && <p className="text-center text-red-500 p-2">{errorUsers}</p>}

                                {!loadingUsers && !errorUsers && (
                                    <table className="w-full border-collapse border border-gray-600 text-sm text-indigo-100">
                                        <thead>
                                        <tr className="bg-indigo-900">
                                            <th className="border border-gray-600 px-2 py-1 text-left">Name</th>
                                            <th className="border border-gray-600 px-2 py-1 text-left">Email</th>
                                            <th className="border border-gray-600 px-2 py-1 text-left">Status</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {filteredUsers.length === 0 && (
                                            <tr>
                                                <td colSpan={3} className="text-center p-2 text-gray-500">
                                                    No users found.
                                                </td>
                                            </tr>
                                        )}
                                        {filteredUsers.map((user) => (
                                            <tr
                                                key={user.userId}
                                                onClick={() => onSelectUser(user)}
                                                className={`cursor-pointer hover:bg-indigo-700 ${
                                                    selectedUser?.userId === user.userId ? "bg-indigo-800 font-semibold" : ""
                                                }`}
                                            >
                                                <td className="border border-gray-600 px-2 py-1">
                                                    {user.firstName} {user.lastName}
                                                </td>
                                                <td className="border border-gray-600 px-2 py-1">{user.email}</td>
                                                <td className="border border-gray-600 px-2 py-1">{pretty(user.status)}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>

                        {/* Right panel: Form */}
                        <div className="col-span-2 bg-gray-800 p-8 rounded-lg shadow-lg text-indigo-100">
                            <h2 className="text-2xl font-semibold mb-6">Customer / User Details</h2>
                            <form onSubmit={handleSubmit}>
                                <div className="grid grid-cols-2 gap-6">
                                    <InputField
                                        label="First Name"
                                        type="text"
                                        name="firstName"
                                        value={formData.firstName ?? ""}
                                        onChange={handleChange}
                                        placeholder="Enter first name"
                                    />

                                    <InputField
                                        label="Last Name"
                                        type="text"
                                        name="lastName"
                                        value={formData.lastName ?? ""}
                                        onChange={handleChange}
                                        placeholder="Enter last name"
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
                                    />

                                    <InputField
                                        label="Email"
                                        type="email"
                                        name="email"
                                        value={formData.email ?? ""}
                                        onChange={handleChange}
                                        placeholder="Enter email"
                                    />

                                    <InputField
                                        label="Phone"
                                        type="tel"
                                        name="phone"
                                        value={formData.phone ?? ""}
                                        onChange={handleChange}
                                        placeholder="Enter phone number"
                                    />

                                    <div className="col-span-2">
                                        <InputField
                                            label="Address"
                                            type="text"
                                            name="address"
                                            value={formData.address ?? ""}
                                            onChange={handleChange}
                                            placeholder="Enter address"
                                        />
                                    </div>

                                    <InputField
                                        label="Date of Birth"
                                        type="date"
                                        name="dateOfBirth"
                                        value={formData.dateOfBirth ?? ""}
                                        onChange={handleChange}
                                    />
                                </div>

                                <div className="mt-6">
                                    <button
                                        type="submit"
                                        className="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700 transition"
                                    >
                                        Save Customer
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

export default CustomerRegistration;
