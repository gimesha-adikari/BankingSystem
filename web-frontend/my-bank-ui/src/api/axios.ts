import axios from "axios";

/** Normalized error shape your UI can rely on */
export type NormalizedError = {
    message: string;
    code: string;
    status: number;
    violations?: Record<string, string>;
};

/* ───────── helpers (type guards, no `any`) ───────── */
function isRecord(v: unknown): v is Record<string, unknown> {
    return !!v && typeof v === "object" && !Array.isArray(v);
}

type FieldErrorItem = { field: string; message: string };
function isFieldErrorItem(v: unknown): v is FieldErrorItem {
    return isRecord(v) && typeof v.field === "string" && typeof v.message === "string";
}

/* ───────── axios instance ───────── */
const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || "",
    withCredentials: false,
    headers: { "Content-Type": "application/json" },
});

/* Attach token */
api.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
    if (token) {
        (config.headers ??= {}).Authorization = `Bearer ${token}`;
    }
    return config;
});

/* Normalize errors into { message, code, status, violations? } */
api.interceptors.response.use(
    (r) => r,
    (error: unknown) => {
        // Not an Axios error: likely network or CORS
        if (!axios.isAxiosError(error)) {
            const normalized: NormalizedError = {
                message: "Network error",
                code: "ERR_NETWORK",
                status: 0,
            };
            return Promise.reject<NormalizedError>(normalized);
        }

        const status = error.response?.status ?? 0;
        const data = error.response?.data as Record<string, unknown> | undefined;

        const message =
            (typeof data?.message === "string" && data.message) ||
            (typeof data?.detail === "string" && data.detail) ||
            (typeof data?.title === "string" && data.title) ||
            error.message ||
            "Request failed";

        const code = (typeof data?.code === "string" && data.code) || "ERR_GENERIC";

        const fieldErrors: Record<string, string> = {};

        const maybeErrors = data?.errors;
        if (Array.isArray(maybeErrors)) {
            for (const item of maybeErrors) {
                if (isFieldErrorItem(item)) {
                    fieldErrors[item.field] = item.message;
                }
            }
        }

        const maybeViolations = data?.violations;
        if (isRecord(maybeViolations)) {
            for (const [k, v] of Object.entries(maybeViolations)) {
                if (typeof v === "string") fieldErrors[k] = v;
            }
        }

        const normalized: NormalizedError = {
            message,
            code,
            status,
            violations: Object.keys(fieldErrors).length ? fieldErrors : undefined,
        };

        return Promise.reject<NormalizedError>(normalized);
    }
);

export default api;
