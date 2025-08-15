import { useState, useEffect, useMemo } from "react";
import Sidebar from "../../components/Sidebar";
import InputField from "../../components/InputField";
import SelectField from "../../components/SelectField";
import { useAlert } from "@/contexts/use-alert";

type Gender = "MALE" | "FEMALE" | "OTHER";
type Status = "ACTIVE" | "INACTIVE" | "PENDING";
type Role = "ADMIN" | "MANAGER" | "TELLER" | "CUSTOMER";

interface User {
    userId: string;
    firstName: string;
    lastName: string;
    gender: Gender;
    email: string;
    phone: string;
    address: string;
    dateOfBirth: string;
    role: Role;
    status: Status;
}

interface Employee {
    employeeId: string;
    firstName: string;
    lastName: string;
    gender: Gender;
    email: string;
    phone: string;
    address: string;
    dateOfBirth: string;
    status: Status;
    department?: string;
    role?: string | null;
    createdAt: string;
    userId: string;
    managerId?: string | null;
}

function useDebounce(value: string, delay: number) {
    const [debouncedValue, setDebouncedValue] = useState(value);
    useEffect(() => {
        const handler = setTimeout(() => setDebouncedValue(value), delay);
        return () => clearTimeout(handler);
    }, [value, delay]);
    return debouncedValue;
}

const formatDateForInput = (dateStr: string) => {
    // Expecting ISO string or yyyy-mm-dd format, fallback to empty string if invalid
    if (!dateStr) return "";
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return "";
    return d.toISOString().substring(0, 10); // yyyy-MM-dd
};

const EmployeeManagement = () => {
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [employeeSearch, setEmployeeSearch] = useState("");
    const [userSearch, setUserSearch] = useState("");

    const [selectedUser, setSelectedUser] = useState<User | null>(null);
    const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);

    const [formData, setFormData] = useState({
        userId: "",
        firstName: "",
        lastName: "",
        gender: "MALE" as Gender,
        email: "",
        phone: "",
        address: "",
        dateOfBirth: "",
        status: "ACTIVE" as Status,
        department: "",
        role: "" as Role | "",
        managerId: "" as string | "",
    });

    const [employees, setEmployees] = useState<Employee[]>([]);
    const [users, setUsers] = useState<User[]>([]);

    const [loadingUsers, setLoadingUsers] = useState(false);
    const [loadingEmployees, setLoadingEmployees] = useState(false);

    const [errorUsers, setErrorUsers] = useState<string | null>(null);
    const [errorEmployees, setErrorEmployees] = useState<string | null>(null);

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

    // Fetch Users
    useEffect(() => {
        if (debouncedUserSearch.length === 0 || debouncedUserSearch.length >= 3) {
            setLoadingUsers(true);
            setErrorUsers(null);

            const query = debouncedUserSearch
                ? `?search=${encodeURIComponent(debouncedUserSearch)}`
                : "";

            fetch(`/api/v1/users/all${query}`, {
                method: "GET",
                headers: getAuthHeaders(),
                credentials: "include",
            })
                .then((res) => {
                    if (res.status === 401 || res.status === 403)
                        throw new Error("Unauthorized. Please login again.");
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
            if (debouncedUserSearch.length > 0 && debouncedUserSearch.length < 3)
                setUsers([]);
        }
    }, [debouncedUserSearch]);

    // Fetch Employees
    const loadEmployees = () => {
        setLoadingEmployees(true);
        setErrorEmployees(null);

        fetch("/api/v1/access-control/employees", {
            method: "GET",
            headers: getAuthHeaders(),
            credentials: "include",
        })
            .then((res) => {
                if (res.status === 401 || res.status === 403)
                    throw new Error("Unauthorized. Please login again.");
                if (!res.ok) throw new Error(`Failed to fetch employees: ${res.status}`);
                return res.json();
            })
            .then((data: Employee[]) => {
                setEmployees(data);
                setLoadingEmployees(false);
            })
            .catch((e) => {
                setErrorEmployees(e.message || "Failed to load employees.");
                showAlert(e.message || "Failed to load employees.");
                setLoadingEmployees(false);
            });
    };

    useEffect(() => {
        loadEmployees();
    }, []);

    const filteredEmployees = useMemo(() => {
        const lowered = employeeSearch.toLowerCase();
        return employees
            .filter(
                (emp) =>
                    emp.firstName.toLowerCase().includes(lowered) ||
                    emp.lastName.toLowerCase().includes(lowered) ||
                    emp.email.toLowerCase().includes(lowered)
            )
            .slice(0, 5);
    }, [employeeSearch, employees]);

    const filteredUsers = useMemo(() => users.slice(0, 5), [users]);

    // Filter managers safely
    const managerOptions = useMemo(() => {
        return employees
            .filter(
                (emp) =>
                    typeof emp.role === "string" &&
                    emp.role.toUpperCase() === "MANAGER"
            )
            .map((manager) => ({
                value: manager.employeeId,
                label: `${manager.firstName} ${manager.lastName}`,
            }));
    }, [employees]);

    const onSelectUser = (user: User) => {
        setSelectedUser(user);
        setSelectedEmployee(null);
        setFormData({
            ...formData,
            userId: user.userId,
            firstName: user.firstName,
            lastName: user.lastName,
            gender: user.gender,
            email: user.email,
            phone: user.phone,
            address: user.address,
            dateOfBirth: formatDateForInput(user.dateOfBirth),
            status: user.status,
            department: "",
            role: user.role,
            managerId: "",
        });
    };

    const onSelectEmployee = (employee: Employee) => {
        setSelectedEmployee(employee);
        setSelectedUser(null);
        setFormData({
            ...formData,
            userId: employee.userId,
            firstName: employee.firstName,
            lastName: employee.lastName,
            gender: employee.gender,
            email: employee.email,
            phone: employee.phone,
            address: employee.address,
            dateOfBirth: formatDateForInput(employee.dateOfBirth),
            status: employee.status,
            department: employee.department || "",
            role: (employee.role as Role) || "",
            managerId: employee.managerId || "",
        });
    };

    const handleChange = (
        e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
    ) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }));
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        // Basic validation
        const requiredFields: (keyof typeof formData)[] = [
            "userId",
            "firstName",
            "lastName",
            "email",
            "phone",
            "address",
            "dateOfBirth",
            "status",
            "gender",
            "role",
        ];
        for (const field of requiredFields) {
            if (!formData[field]) {
                showAlert(`${field} is required.`);
                return;
            }
        }

        try {
            const url = `/api/v1/access-control/roles/${formData.userId}`;

            const payload = {
                roleName: formData.role,
                email: formData.email,
                firstName: formData.firstName,
                lastName: formData.lastName,
                phone: formData.phone,
                department: formData.department,
                managerId: formData.managerId || null,
                status: formData.status,
                gender: formData.gender,
                dateOfBirth: formData.dateOfBirth,
                address: formData.address,
            };

            const response = await fetch(url, {
                method: "PUT",
                headers: getAuthHeaders(),
                credentials: "include",
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                showAlert(
                    `Failed to save employee: ${
                        errorData?.message || response.statusText || "Unknown error"
                    }`
                );
                return;
            }

            showAlert("Employee saved successfully!", "success");

            setSelectedEmployee(null);
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
                department: "",
                role: "",
                managerId: "",
            });

            loadEmployees();
        } catch (error: any) {
            showAlert("An error occurred while saving: " + (error?.message || "Unknown error"));
        }
    };

    return (
        <div className="flex h-screen bg-gradient-to-b from-gray-600 to-gray-900">
            <Sidebar isOpen={sidebarOpen} role="ADMIN" />

            <div className="flex-1 flex flex-col relative">
                <header className="bg-gray-900 shadow-md py-4 px-6 flex justify-between items-center md:hidden">
                    <button
                        onClick={() => setSidebarOpen(!sidebarOpen)}
                        className="text-indigo-400 text-2xl focus:outline-none"
                        aria-label="Toggle sidebar"
                    >
                        ☰
                    </button>
                    <h1 className="text-lg font-semibold text-indigo-300">Employee Management</h1>
                </header>

                <main className="flex-1 p-6 overflow-auto text-indigo-100">
                    <button
                        onClick={() => window.history.back()}
                        aria-label="Go back"
                        className="absolute top-4 right-4 text-gray-300 hover:text-white rounded-full p-2 focus:outline-none focus:ring-2 focus:ring-indigo-400"
                        title="Go back"
                    >
                        ✕
                    </button>

                    <h1 className="text-3xl font-bold mb-8 text-indigo-200">Employee Management</h1>

                    <div className="grid grid-cols-3 gap-8">
                        {/* Employees */}
                        <div className="col-span-1 space-y-8">
                            <div>
                                <h2 className="text-xl font-semibold mb-3 text-indigo-300">Employees</h2>
                                <input
                                    type="text"
                                    placeholder="Search employees by name or email"
                                    value={employeeSearch}
                                    onChange={(e) => setEmployeeSearch(e.target.value)}
                                    className="w-full mb-2 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-gray-700 text-indigo-100 border-gray-600"
                                />

                                {loadingEmployees && (
                                    <p className="text-center text-gray-400 p-2">Loading employees...</p>
                                )}
                                {errorEmployees && (
                                    <p className="text-center text-red-500 p-2">{errorEmployees}</p>
                                )}

                                {!loadingEmployees && !errorEmployees && (
                                    <table className="w-full border-collapse border border-gray-600 text-sm text-indigo-100">
                                        <thead>
                                        <tr className="bg-indigo-900">
                                            <th className="border border-gray-600 px-2 py-1 text-left">Name</th>
                                            <th className="border border-gray-600 px-2 py-1 text-left">Email</th>
                                            <th className="border border-gray-600 px-2 py-1 text-left">Status</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {filteredEmployees.length === 0 && (
                                            <tr>
                                                <td colSpan={3} className="text-center p-2 text-gray-500">
                                                    No employees found.
                                                </td>
                                            </tr>
                                        )}
                                        {filteredEmployees.map((emp) => (
                                            <tr
                                                key={emp.employeeId}
                                                onClick={() => onSelectEmployee(emp)}
                                                className={`cursor-pointer hover:bg-indigo-700 ${
                                                    selectedEmployee?.employeeId === emp.employeeId
                                                        ? "bg-indigo-800 font-semibold"
                                                        : ""
                                                }`}
                                            >
                                                <td className="border border-gray-600 px-2 py-1">
                                                    {emp.firstName} {emp.lastName}
                                                </td>
                                                <td className="border border-gray-600 px-2 py-1">{emp.email}</td>
                                                <td className="border border-gray-600 px-2 py-1">{pretty(emp.status)}</td>
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
                                    placeholder="Search users by name or email"
                                    value={userSearch}
                                    onChange={(e) => setUserSearch(e.target.value)}
                                    className="w-full mb-2 px-3 py-2 border rounded focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-gray-700 text-indigo-100 border-gray-600"
                                />

                                {loadingUsers && (
                                    <p className="text-center text-gray-400 p-2">Loading users...</p>
                                )}
                                {errorUsers && (
                                    <p className="text-center text-red-500 p-2">{errorUsers}</p>
                                )}

                                {!loadingUsers && !errorUsers && (
                                    <table className="w-full border-collapse border border-gray-600 text-sm text-indigo-100">
                                        <thead>
                                        <tr className="bg-indigo-900">
                                            <th className="border border-gray-600 px-2 py-1 text-left">Name</th>
                                            <th className="border border-gray-600 px-2 py-1 text-left">Email</th>
                                            <th className="border border-gray-600 px-2 py-1 text-left">Role</th>
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
                                                <td className="border border-gray-600 px-2 py-1">{pretty(user.role)}</td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>

                        {/* Form */}
                        <form
                            onSubmit={handleSubmit}
                            className="col-span-2 bg-gray-800 rounded-md p-6 shadow-lg space-y-6"
                            noValidate
                        >
                            <h2 className="text-2xl font-semibold text-indigo-300 mb-4">
                                {selectedEmployee
                                    ? `Edit Employee: ${selectedEmployee.firstName} ${selectedEmployee.lastName}`
                                    : selectedUser
                                        ? `Add Employee for User: ${selectedUser.firstName} ${selectedUser.lastName}`
                                        : "Select a user or employee to edit"}
                            </h2>



                            <SelectField
                                id="managerId"
                                name="managerId"
                                label="Manager"
                                value={formData.managerId}
                                options={managerOptions}
                                onChange={handleChange}
                                disabled // manager cannot be changed manually here
                            />

                            <button
                                type="submit"
                                disabled={!formData.userId}
                                className={`w-full py-3 rounded-md text-white font-semibold transition-colors ${
                                    formData.userId
                                        ? "bg-indigo-600 hover:bg-indigo-700"
                                        : "bg-indigo-400 cursor-not-allowed"
                                }`}
                            >
                                Save Employee
                            </button>
                        </form>
                    </div>
                </main>
            </div>
        </div>
    );
};

export default EmployeeManagement;
