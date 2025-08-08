import { useState, useMemo, useEffect } from "react";
import InputField from "../../components/InputField";
import SelectField from "../../components/SelectField";

type Gender = "Male" | "Female" | "Other";
type Status = "Active" | "Inactive" | "Pending";

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
    user: User;
    createdAt: string;
}

const CustomerRegistration = () => {
    const [customerSearch, setCustomerSearch] = useState("");
    const [userSearch, setUserSearch] = useState("");

    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);

    const [formData, setFormData] = useState<User>({
        userId: "",
        firstName: "",
        lastName: "",
        gender: "Male",
        email: "",
        phone: "",
        address: "",
        dateOfBirth: "",
        status: "Active",
    });

    // Backend loaded data states
    const [customers, setCustomers] = useState<Customer[]>([]);
    const [users, setUsers] = useState<User[]>([]);

    const [loadingUsers, setLoadingUsers] = useState(false);
    const [loadingCustomers, setLoadingCustomers] = useState(false);

    const [errorUsers, setErrorUsers] = useState<string | null>(null);
    const [errorCustomers, setErrorCustomers] = useState<string | null>(null);

    // Fetch users from backend
    useEffect(() => {
        setLoadingUsers(true);
        setErrorUsers(null);

        fetch("/api/v1/users/all")
            .then((res) => {
                if (!res.ok) throw new Error(`Failed to fetch users: ${res.status}`);
                return res.json();
            })
            .then((data: User[]) => {
                setUsers(data);
                setLoadingUsers(false);
            })
            .catch((e) => {
                setErrorUsers(e.message);
                setLoadingUsers(false);
            });
    }, []);

    // Fetch customers from backend
    useEffect(() => {
        setLoadingCustomers(true);
        setErrorCustomers(null);

        fetch("/api/v1/customers")
            .then((res) => {
                if (!res.ok) throw new Error(`Failed to fetch customers: ${res.status}`);
                return res.json();
            })
            .then((data: Customer[]) => {
                setCustomers(data);
                setLoadingCustomers(false);
            })
            .catch((e) => {
                setErrorCustomers(e.message);
                setLoadingCustomers(false);
            });
    }, []);

    // Filter customers by search query
    const filteredCustomers = useMemo(() => {
        const lowered = customerSearch.toLowerCase();
        return customers
            .filter((c) => {
                const user = c.user;
                return (
                    user.firstName.toLowerCase().includes(lowered) ||
                    user.lastName.toLowerCase().includes(lowered) ||
                    user.email.toLowerCase().includes(lowered)
                );
            })
            .slice(0, 5);
    }, [customerSearch, customers]);

    // Filter users by search query
    const filteredUsers = useMemo(() => {
        const lowered = userSearch.toLowerCase();
        return users
            .filter((u) => {
                return (
                    u.firstName.toLowerCase().includes(lowered) ||
                    u.lastName.toLowerCase().includes(lowered) ||
                    u.email.toLowerCase().includes(lowered)
                );
            })
            .slice(0, 5);
    }, [userSearch, users]);

    const onSelectUser = (user: User) => {
        setSelectedUser(user);
        setSelectedCustomer(null);
        setFormData(user);
    };

    const onSelectCustomer = (customer: Customer) => {
        setSelectedCustomer(customer);
        setSelectedUser(null);
        setFormData(customer.user);
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    return (
        <div className="p-8 bg-gray-50 min-h-screen max-w-7xl mx-auto relative">
            {/* Close button */}
            <button
                onClick={() => window.history.back()}
                aria-label="Go back"
                className="absolute top-4 right-4 text-gray-600 hover:text-gray-900 rounded-full p-2 focus:outline-none focus:ring-2 focus:ring-indigo-400"
                title="Go back"
            >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
            </button>

            <h1 className="text-3xl font-bold mb-8 text-gray-800">Customer Registration</h1>

            <div className="grid grid-cols-3 gap-8">
                {/* Left panel */}
                <div className="col-span-1 space-y-8">
                    {/* Customers */}
                    <div>
                        <h2 className="text-xl font-semibold mb-3">Customers</h2>
                        <input
                            type="text"
                            placeholder="Search customers by name or email"
                            value={customerSearch}
                            onChange={(e) => setCustomerSearch(e.target.value)}
                            className="w-full mb-2 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-indigo-400"
                        />

                        {loadingCustomers && <p className="text-center text-gray-500 p-2">Loading customers...</p>}
                        {errorCustomers && <p className="text-center text-red-500 p-2">{errorCustomers}</p>}

                        {!loadingCustomers && !errorCustomers && (
                            <table className="w-full border-collapse border border-gray-300 text-sm">
                                <thead>
                                <tr className="bg-indigo-100">
                                    <th className="border border-gray-300 px-2 py-1">Name</th>
                                    <th className="border border-gray-300 px-2 py-1">Email</th>
                                    <th className="border border-gray-300 px-2 py-1">Status</th>
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
                                        className={`cursor-pointer hover:bg-indigo-200 ${
                                            selectedCustomer?.customerId === cust.customerId ? "bg-indigo-300 font-semibold" : ""
                                        }`}
                                    >
                                        <td className="border border-gray-300 px-2 py-1">
                                            {cust.user.firstName} {cust.user.lastName}
                                        </td>
                                        <td className="border border-gray-300 px-2 py-1">{cust.user.email}</td>
                                        <td className="border border-gray-300 px-2 py-1">{cust.user.status}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )}
                    </div>

                    {/* Users */}
                    <div>
                        <h2 className="text-xl font-semibold mb-3">Users</h2>
                        <input
                            type="text"
                            placeholder="Search users by name or email"
                            value={userSearch}
                            onChange={(e) => setUserSearch(e.target.value)}
                            className="w-full mb-2 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-indigo-400"
                        />

                        {loadingUsers && <p className="text-center text-gray-500 p-2">Loading users...</p>}
                        {errorUsers && <p className="text-center text-red-500 p-2">{errorUsers}</p>}

                        {!loadingUsers && !errorUsers && (
                            <table className="w-full border-collapse border border-gray-300 text-sm">
                                <thead>
                                <tr className="bg-indigo-100">
                                    <th className="border border-gray-300 px-2 py-1">Name</th>
                                    <th className="border border-gray-300 px-2 py-1">Email</th>
                                    <th className="border border-gray-300 px-2 py-1">Status</th>
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
                                        className={`cursor-pointer hover:bg-indigo-200 ${
                                            selectedUser?.userId === user.userId ? "bg-indigo-300 font-semibold" : ""
                                        }`}
                                    >
                                        <td className="border border-gray-300 px-2 py-1">
                                            {user.firstName} {user.lastName}
                                        </td>
                                        <td className="border border-gray-300 px-2 py-1">{user.email}</td>
                                        <td className="border border-gray-300 px-2 py-1">{user.status}</td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>

                {/* Right panel */}
                <div className="col-span-2 bg-white p-8 rounded-lg shadow-lg">
                    <h2 className="text-2xl font-semibold mb-6">Customer / User Details</h2>
                    <form>
                        <div className="grid grid-cols-2 gap-6">
                            <InputField
                                label="First Name"
                                type="text"
                                name="firstName"
                                value={formData.firstName}
                                onChange={handleChange}
                                placeholder="Enter first name"
                            />

                            <InputField
                                label="Last Name"
                                type="text"
                                name="lastName"
                                value={formData.lastName}
                                onChange={handleChange}
                                placeholder="Enter last name"
                            />

                            <SelectField
                                label="Gender"
                                name="gender"
                                value={formData.gender}
                                onChange={handleChange}
                                options={[
                                    { value: "Male", label: "Male" },
                                    { value: "Female", label: "Female" },
                                    { value: "Other", label: "Other" },
                                ]}
                            />

                            <SelectField
                                label="Status"
                                name="status"
                                value={formData.status}
                                onChange={handleChange}
                                options={[
                                    { value: "Active", label: "Active" },
                                    { value: "Inactive", label: "Inactive" },
                                    { value: "Pending", label: "Pending" },
                                ]}
                            />

                            <InputField
                                label="Email"
                                type="email"
                                name="email"
                                value={formData.email}
                                onChange={handleChange}
                                placeholder="Enter email"
                            />

                            <InputField
                                label="Phone"
                                type="tel"
                                name="phone"
                                value={formData.phone}
                                onChange={handleChange}
                                placeholder="Enter phone number"
                            />

                            <div className="col-span-2">
                                <InputField
                                    label="Address"
                                    type="text"
                                    name="address"
                                    value={formData.address}
                                    onChange={handleChange}
                                    placeholder="Enter address"
                                />
                            </div>

                            <InputField
                                label="Date of Birth"
                                type="date"
                                name="dateOfBirth"
                                value={formData.dateOfBirth}
                                onChange={handleChange}
                            />
                        </div>

                        <div className="mt-6">
                            <button
                                type="button"
                                className="bg-indigo-600 text-white px-6 py-2 rounded-lg hover:bg-indigo-700 transition"
                            >
                                Save Customer
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default CustomerRegistration;
