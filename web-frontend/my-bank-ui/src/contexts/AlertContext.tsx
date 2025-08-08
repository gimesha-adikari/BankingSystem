// src/context/AlertContext.tsx
import React, { createContext, useContext, useState, ReactNode } from "react";

type AlertType = "success" | "error" | "info";

interface AlertContextType {
    message: string;
    type: AlertType;
    visible: boolean;
    showAlert: (message: string, type?: AlertType, duration?: number) => void;
    hideAlert: () => void;
}

const AlertContext = createContext<AlertContextType | undefined>(undefined);

export const AlertProvider = ({ children }: { children: ReactNode }) => {
    const [message, setMessage] = useState("");
    const [type, setType] = useState<AlertType>("info");
    const [visible, setVisible] = useState(false);

    const showAlert = (msg: string, alertType: AlertType = "info", duration = 3000) => {
        setMessage(msg);
        setType(alertType);
        setVisible(true);

        // Auto-hide after duration
        setTimeout(() => {
            setVisible(false);
        }, duration);
    };

    const hideAlert = () => setVisible(false);

    return (
        <AlertContext.Provider value={{ message, type, visible, showAlert, hideAlert }}>
            {children}
        </AlertContext.Provider>
    );
};

export const useAlert = (): AlertContextType => {
    const context = useContext(AlertContext);
    if (!context) {
        throw new Error("useAlert must be used within an AlertProvider");
    }
    return context;
};
