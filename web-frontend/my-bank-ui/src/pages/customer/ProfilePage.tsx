import {useEffect, useState} from "react";
import axios from "axios";
import Sidebar from "../../components/Sidebar";
import ChangePasswordModal from "../../models/ChangePasswordModal";
import {useAlert} from "../../contexts/AlertContext";
import {useUsernameAvailability} from "../../hooks/useUsernameAvailability";
import {useAuth} from "../../contexts/AuthContext";
import PasswordVerification from "../../models/PasswordVerification";

interface UserProfile {
    userId: string;
    username: string;
    firstName: string;
    lastName: string;
    address: string;
    city: string;
    state: string;
    country: string;
    postalCode: string;
    homeNumber: string;
    workNumber: string;
    officeNumber: string;
    mobileNumber: string;
    email: string;
    roleName: string;
}

type EditableFields =
    | "username"
    | "email"
    | "firstName"
    | "lastName"
    | "address"
    | "city"
    | "state"
    | "country"
    | "postalCode"
    | "homeNumber"
    | "workNumber"
    | "officeNumber"
    | "mobileNumber";

const Profile = () => {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [showPasswordModal, setShowPasswordModal] = useState(false);

    const [editingField, setEditingField] = useState<EditableFields | null>(null);
    const [tempValue, setTempValue] = useState<string>("");

    // For username update password verification modal
    const [passwordModalForUsername, setPasswordModalForUsername] = useState(false);
    const [pendingUsername, setPendingUsername] = useState<string | null>(null);
    const [pendingEditingField, setPendingEditingField] = useState<EditableFields | null>(null);

    const isEditingUsername = editingField === "username";
    const {available: usernameAvailable, loading: checkingUsername} = useUsernameAvailability(
        isEditingUsername ? tempValue : ""
    );

    const {showAlert} = useAlert();
    const {login} = useAuth();

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const token = localStorage.getItem("token");
                const response = await axios.get("/api/v1/users/me", {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });
                setProfile(response.data);
            } catch (err: any) {
                setError("Failed to load profile.");
                console.error(err);
            } finally {
                setLoading(false);
            }
        };

        fetchProfile();
    }, []);

    const startEditing = (field: EditableFields) => {
        if (!profile) return;
        setEditingField(field);
        setTempValue(profile[field] || "");
    };

    const cancelEditing = () => {
        setEditingField(null);
        setTempValue("");
    };

    const saveEditing = async () => {
        if (!profile || !editingField) return;

        if (editingField === "username" && !usernameAvailable) {
            showAlert("Username is not available or too short", "error");
            return;
        }

        if (editingField === "username") {
            setPendingUsername(tempValue);
            setPendingEditingField(editingField);
            setPasswordModalForUsername(true);
            return;
        }

        try {
            const token = localStorage.getItem("token");

            const updatePayload = {
                [editingField]: tempValue,
            };

            await axios.put("/api/v1/users/me", updatePayload, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
            });

            setProfile({
                ...profile,
                [editingField]: tempValue,
            });

            showAlert("Profile updated successfully", "success");
            setEditingField(null);
            setTempValue("");
        } catch (err: any) {
            console.error("Failed to update profile", err);
            showAlert("Failed to update profile. Please try again.", "error");
        }
    };

    const handleUsernamePasswordVerified = async (password: string) => {
        if (!pendingUsername || !pendingEditingField) return;

        try {
            const token = localStorage.getItem("token");

            const updatePayload = {
                [pendingEditingField]: pendingUsername,
                currentPassword: password,
            };

            await axios.put("/api/v1/users/me", updatePayload, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
            });

            const username = pendingUsername;

            const response = await fetch("/api/v1/auth/login", {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify({username, password}),
            });

            if (!response.ok) {
                const errorText = await response.text();
                showAlert("Login failed: " + errorText, "error");
                return;
            }

            const data = await response.json();


            localStorage.setItem("token", data.token);
            localStorage.setItem("username", data.username);
            localStorage.setItem("role", data.role);

            login(data.token, data.username, data.role);

            setProfile({
                ...profile,
                [pendingEditingField]: pendingUsername,
            });

            showAlert("Username updated and re-authenticated successfully", "success");
            setEditingField(null);
            setTempValue("");
        } catch (err: any) {
            console.error("Failed to update username", err);
            showAlert("Failed to update username. Please try again.", "error");
        } finally {
            setPasswordModalForUsername(false);
            setPendingUsername(null);
            setPendingEditingField(null);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center h-screen bg-gray-50">
                <div className="text-gray-600 text-lg animate-pulse">Loading profile...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex justify-center items-center h-screen bg-gray-50">
                <div className="text-red-600 text-lg">{error}</div>
            </div>
        );
    }

    return (
        <div className="flex h-screen bg-gradient-to-b from-gray-700 to-gray-900">
            <Sidebar isOpen={sidebarOpen} role="CUSTOMER"/>

            <div className="flex-1 flex flex-col">
                {/* Mobile Top Bar */}
                <header className="bg-gray-900 shadow-md py-4 px-6 flex justify-between items-center md:hidden">
                    <button
                        onClick={() => setSidebarOpen(!sidebarOpen)}
                        className="text-indigo-400 text-2xl focus:outline-none"
                        aria-label="Toggle sidebar"
                    >
                        ☰
                    </button>
                    <h1 className="text-lg font-semibold text-indigo-300">My Profile</h1>
                </header>

                {/* Main Content */}
                <main className="flex-1 p-10 overflow-auto">
                    <div className="max-w-3xl mx-auto bg-white rounded-3xl shadow-2xl p-12">
                        <h2 className="text-4xl font-extrabold text-blue-900 mb-8 text-center tracking-wide">My
                            Profile</h2>

                        <div className="space-y-7">
                            {[
                                "username",
                                "firstName",
                                "lastName",
                                "email",
                                "address",
                                "city",
                                "state",
                                "country",
                                "postalCode",
                                "homeNumber",
                                "workNumber",
                                "officeNumber",
                                "mobileNumber",
                            ].map((field) => (
                                <div key={field} className="flex items-center gap-4"
                                     aria-label={`Profile field ${field}`}>
                                    <label
                                        htmlFor={field}
                                        className="w-28 text-sm font-semibold text-gray-600 capitalize select-none"
                                    >
                                        {field.replace(/([A-Z])/g, " $1")}
                                    </label>

                                    {editingField === field ? (
                                        <>
                                            <input
                                                id={field}
                                                type="text"
                                                value={tempValue}
                                                onChange={(e) => setTempValue(e.target.value)}
                                                autoFocus
                                                className="flex-1 px-4 py-2 border-2 border-indigo-500 rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-400 text-gray-900 transition"
                                                placeholder={`Enter your ${field}`}
                                            />
                                            {field === "username" && tempValue && (
                                                <p
                                                    className={`text-sm mt-1 ${
                                                        checkingUsername
                                                            ? "text-gray-500"
                                                            : usernameAvailable
                                                                ? "text-green-600"
                                                                : "text-red-600"
                                                    }`}
                                                >
                                                    {checkingUsername
                                                        ? "Checking username..."
                                                        : usernameAvailable
                                                            ? "Username is available"
                                                            : "Username is not available or too short"}
                                                </p>
                                            )}

                                            <button
                                                onClick={saveEditing}
                                                className="bg-indigo-600 text-white px-4 py-2 rounded-xl font-semibold hover:bg-indigo-700 shadow-md transition"
                                                aria-label="Save changes"
                                            >
                                                Save
                                            </button>
                                            <button
                                                onClick={cancelEditing}
                                                className="bg-gray-300 text-gray-700 px-4 py-2 rounded-xl font-semibold hover:bg-gray-400 shadow-md transition"
                                                aria-label="Cancel editing"
                                            >
                                                Cancel
                                            </button>
                                        </>
                                    ) : (
                                        <>
                                            <p className="flex-1 text-lg text-gray-800 select-text break-all">
                                                {profile?.[field as keyof UserProfile] ||
                                                    <span className="italic text-gray-400">Not set</span>}
                                            </p>
                                            <button
                                                onClick={() => startEditing(field as EditableFields)}
                                                className="text-indigo-600 hover:text-indigo-900 underline font-medium text-sm whitespace-nowrap"
                                                aria-label={`Edit ${field}`}
                                            >
                                                Edit
                                            </button>
                                        </>
                                    )}
                                </div>
                            ))}

                            <div className="pt-4 border-t border-gray-200 space-y-4">
                                <div>
                                    <p className="text-sm text-gray-500 font-semibold mb-1">Role</p>
                                    <span
                                        className="inline-block bg-blue-100 text-blue-900 text-sm px-4 py-1 rounded-full font-semibold tracking-wide select-text">
                    {profile?.roleName}
                  </span>
                                </div>

                                <button
                                    onClick={() => setShowPasswordModal(true)}
                                    className="mt-2 px-4 py-2 bg-red-500 text-white rounded-xl font-semibold hover:bg-red-600 transition shadow-md"
                                >
                                    Change Password
                                </button>
                            </div>
                        </div>
                    </div>
                </main>

                {showPasswordModal && <ChangePasswordModal onClose={() => setShowPasswordModal(false)}/>}

                {/* Password verification modal for username change */}
                {passwordModalForUsername && (
                    <div className="fixed inset-0 z-60 flex items-center justify-center backdrop-blur-sm bg-black/40">
                        <div className="bg-white p-6 rounded-xl shadow-lg w-full max-w-sm">
                            <PasswordVerification
                                token={localStorage.getItem("token")}
                                onVerified={handleUsernamePasswordVerified}
                                onCancel={() => setPasswordModalForUsername(false)}
                            />
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};


export default Profile;
