import { useEffect, useState } from "react";
import axios from "axios";
import Sidebar from "../../components/Sidebar";
import ChangePasswordModal from "../../models/ChangePasswordModal";
import { useAlert } from "@/contexts/use-alert";
import { useUsernameAvailability } from "@/hooks/useUsernameAvailability.ts";
import { useAuth } from "@/contexts/auth-context.ts";
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

    const [passwordModalForUsername, setPasswordModalForUsername] = useState(false);
    const [pendingUsername, setPendingUsername] = useState<string | null>(null);
    const [pendingEditingField, setPendingEditingField] = useState<EditableFields | null>(null);

    const isEditingUsername = editingField === "username";
    const { available: usernameAvailable, loading: checkingUsername } = useUsernameAvailability(
        isEditingUsername ? tempValue : ""
    );

    const { showAlert } = useAlert();
    const { login } = useAuth();

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
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username, password }),
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
            <div className="min-h-screen grid place-items-center px-4 text-indigo-100 bg-[#0B0B12] bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)]">
                <div className="w-full max-w-md rounded-2xl bg-white/90 backdrop-blur p-8 shadow-2xl ring-1 ring-black/5">
                    <div className="h-6 w-40 rounded bg-slate-200 animate-pulse mb-4" />
                    <div className="space-y-3">
                        <div className="h-4 w-full rounded bg-slate-200 animate-pulse" />
                        <div className="h-4 w-5/6 rounded bg-slate-200 animate-pulse" />
                        <div className="h-4 w-2/3 rounded bg-slate-200 animate-pulse" />
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="min-h-screen grid place-items-center px-4 text-indigo-100 bg-[#0B0B12] bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)]">
                <div className="w-full max-w-md rounded-2xl bg-white/90 backdrop-blur p-8 shadow-2xl ring-1 ring-black/5 text-center">
                    <h2 className="text-xl font-semibold text-slate-950 mb-2">Something went wrong</h2>
                    <p className="text-slate-700">{error}</p>
                </div>
            </div>
        );
    }

    return (
        <div className="flex h-screen text-indigo-100 bg-[#0B0B12] bg-[radial-gradient(70%_55%_at_50%_-10%,rgba(99,102,241,0.16),transparent)]">
            <Sidebar isOpen={sidebarOpen} role="CUSTOMER" />

            <div className="flex-1 flex flex-col">
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
                        <h1 className="text-base font-semibold text-indigo-200">My Profile</h1>
                    </div>
                </header>

                <main className="flex-1 px-6 py-6 md:px-8 md:py-10 overflow-auto">
                    <div className="max-w-4xl mx-auto">
                        <div className="rounded-2xl ring-1 ring-white/10 bg-white/5 backdrop-blur p-6 md:p-8 shadow-sm motion-safe:animate-[riseIn_.28s_ease-out]">
                            <h2 className="text-3xl md:text-4xl font-bold tracking-tight text-white text-center">
                                My Profile
                            </h2>

                            <div className="mt-8 space-y-6">
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
                                    <div key={field} className="flex flex-col sm:flex-row sm:items-center gap-3">
                                        <label
                                            htmlFor={field}
                                            className="sm:w-40 text-sm font-medium text-indigo-200/90 capitalize"
                                        >
                                            {field.replace(/([A-Z])/g, " $1")}
                                        </label>

                                        {editingField === field ? (
                                            <div className="flex-1 flex flex-col sm:flex-row gap-2 sm:items-center">
                                                <input
                                                    id={field}
                                                    type="text"
                                                    value={tempValue}
                                                    onChange={(e) => setTempValue(e.target.value)}
                                                    autoFocus
                                                    className="flex-1 rounded-xl px-3 py-2 bg-white text-slate-900 placeholder:text-slate-400 ring-1 ring-slate-300 focus:ring-2 focus:ring-indigo-500 outline-none shadow-sm"
                                                    placeholder={`Enter your ${field}`}
                                                />

                                                {field === "username" && tempValue && (
                                                    <span
                                                        className={[
                                                            "text-xs rounded-md px-2 py-1 ring-1",
                                                            checkingUsername
                                                                ? "text-slate-700 bg-slate-100 ring-slate-200"
                                                                : usernameAvailable
                                                                    ? "text-emerald-700 bg-emerald-50 ring-emerald-200"
                                                                    : "text-rose-700 bg-rose-50 ring-rose-200",
                                                        ].join(" ")}
                                                        aria-live="polite"
                                                    >
                            {checkingUsername
                                ? "Checking username..."
                                : usernameAvailable
                                    ? "Username is available"
                                    : "Username is not available or too short"}
                          </span>
                                                )}

                                                <div className="flex gap-2">
                                                    <button
                                                        onClick={saveEditing}
                                                        className="px-4 py-2 rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition shadow-sm"
                                                        aria-label="Save changes"
                                                    >
                                                        Save
                                                    </button>
                                                    <button
                                                        onClick={cancelEditing}
                                                        className="px-4 py-2 rounded-xl bg-slate-200 text-slate-800 hover:bg-slate-300 focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                                                        aria-label="Cancel editing"
                                                    >
                                                        Cancel
                                                    </button>
                                                </div>
                                            </div>
                                        ) : (
                                            <div className="flex-1 flex items-center gap-3">
                                                <p className="flex-1 text-base text-indigo-100/95 break-all">
                                                    {profile?.[field as keyof UserProfile] || (
                                                        <span className="italic text-indigo-300/70">Not set</span>
                                                    )}
                                                </p>
                                                <button
                                                    onClick={() => startEditing(field as EditableFields)}
                                                    className="text-sm font-medium text-indigo-300 hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded px-2 py-1 hover:bg-white/10 transition whitespace-nowrap"
                                                    aria-label={`Edit ${field}`}
                                                >
                                                    Edit
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                ))}

                                <div className="pt-6 mt-2 border-t border-white/10 space-y-4">
                                    <div>
                                        <p className="text-xs uppercase tracking-wide text-indigo-200/70 mb-1">Role</p>
                                        <span className="inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold bg-indigo-500/15 text-indigo-200 ring-1 ring-indigo-500/30">
                      {profile?.roleName}
                    </span>
                                    </div>

                                    <button
                                        onClick={() => setShowPasswordModal(true)}
                                        className="px-4 py-2 rounded-xl bg-rose-600 text-white hover:bg-rose-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-500 transition shadow-sm"
                                    >
                                        Change Password
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>

                {showPasswordModal && <ChangePasswordModal onClose={() => setShowPasswordModal(false)} />}

                {passwordModalForUsername && (
                    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 backdrop-blur-sm">
                        <div className="bg-white rounded-2xl p-6 w-full max-w-sm shadow-2xl ring-1 ring-black/5">
                            <PasswordVerification
                                token={localStorage.getItem("token")}
                                onVerified={handleUsernamePasswordVerified}
                                onCancel={() => setPasswordModalForUsername(false)}
                            />
                        </div>
                    </div>
                )}
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

export default Profile;
