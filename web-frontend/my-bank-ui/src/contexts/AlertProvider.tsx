import React, { useState, useRef, useCallback, useMemo, useEffect } from "react";
import { AlertContext, type AlertContextType, type AlertType } from "./alert-context";

const DEFAULT_DURATION = 4000;

export default function AlertProvider({ children }: { children: React.ReactNode }) {
    const [message, setMessage] = useState<AlertContextType["message"]>("");
    const [type, setType] = useState<AlertType>("info");
    const [visible, setVisible] = useState(false);
    const timerRef = useRef<number | null>(null);

    const clearTimer = () => {
        if (timerRef.current != null) {
            window.clearTimeout(timerRef.current);
            timerRef.current = null;
        }
    };

    const hideAlert = useCallback(() => {
        clearTimer();
        setVisible(false);
    }, []);

    const showAlert = useCallback(
        (msg: AlertContextType["message"], alertType: AlertType = "info", duration: number | null = DEFAULT_DURATION) => {
            clearTimer();
            setMessage(msg);
            setType(alertType);
            setVisible(true);
            if (duration && duration > 0) {
                timerRef.current = window.setTimeout(() => {
                    setVisible(false);
                    timerRef.current = null;
                }, duration);
            }
        },
        []
    );

    useEffect(() => {
        return () => clearTimer();
    }, []);

    const value = useMemo(
        () => ({ message, type, visible, showAlert, hideAlert }),
        [message, type, visible, showAlert, hideAlert]
    );

    return <AlertContext.Provider value={value}>{children}</AlertContext.Provider>;
}
