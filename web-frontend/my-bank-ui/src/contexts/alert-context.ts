import { createContext } from "react";
import type { ReactNode } from "react";

export type AlertType = "success" | "error" | "info" | "warning";

export interface AlertContextType {
    message: ReactNode;
    type: AlertType;
    visible: boolean;
    showAlert: (message: ReactNode, type?: AlertType, duration?: number | null) => void;
    hideAlert: () => void;
}

export const AlertContext = createContext<AlertContextType | undefined>(undefined);
