import React from "react";
import { useAlert } from "../contexts/AlertContext";
import { CheckCircle, XCircle, Info, AlertTriangle, X } from "lucide-react";

const Alert = () => {
    const { message, type, visible, hideAlert } = useAlert();

    if (!visible) return null;

    const typeStyles = {
        success: {
            bg: "bg-green-100 border-green-500 text-green-700",
            icon: <CheckCircle className="w-5 h-5 text-green-500" />,
        },
        error: {
            bg: "bg-red-100 border-red-500 text-red-700",
            icon: <XCircle className="w-5 h-5 text-red-500" />,
        },
        info: {
            bg: "bg-blue-100 border-blue-500 text-blue-700",
            icon: <Info className="w-5 h-5 text-blue-500" />,
        },
        warning: {
            bg: "bg-yellow-100 border-yellow-500 text-yellow-700",
            icon: <AlertTriangle className="w-5 h-5 text-yellow-500" />,
        }
    };

    const style = typeStyles[type] || typeStyles.info;

    return (
        <div className={`fixed top-5 right-5 z-50 min-w-[250px] max-w-sm px-4 py-3 border-l-4 rounded-md shadow-md flex items-start gap-3 animate-fade-in-down ${style.bg}`}>
            <div className="pt-1">{style.icon}</div>
            <div className="flex-1">
                <p className="text-sm font-medium">{message}</p>
            </div>
            <button onClick={hideAlert} className="text-xl font-bold leading-none hover:opacity-70">
                <X className="w-4 h-4" />
            </button>
        </div>
    );
};

export default Alert;
