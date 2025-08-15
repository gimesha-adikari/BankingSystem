import { useContext } from "react";
import { AlertContext, type AlertContextType } from "./alert-context";

export function useAlert(): AlertContextType {
    const ctx = useContext(AlertContext);
    if (!ctx) throw new Error("useAlert must be used within an AlertProvider");
    return ctx;
}
