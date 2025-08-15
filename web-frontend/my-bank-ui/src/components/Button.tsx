import React from "react";

type Props = {
    children: React.ReactNode;
    type?: "button" | "submit" | "reset";
    onClick?: () => void;
    className?: string;
    disabled?: boolean;
};

const Button = ({
                    children,
                    type = "button",
                    onClick,
                    className = "",
                    disabled = false,
                }: Props) => (
    <button
        type={type}
        onClick={onClick}
        disabled={disabled}
        className={`w-full font-semibold py-2.5 px-5 rounded-lg shadow-sm transition-all duration-200
            focus:outline-none focus:ring-2 focus:ring-offset-1
            ${disabled
            ? "bg-blue-300 text-white cursor-not-allowed"
            : "bg-blue-600 hover:bg-blue-700 text-white focus:ring-blue-400"}
            ${className}`}
    >
        {children}
    </button>
);

export default Button;
