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
        className={`font-semibold py-2 px-4 rounded-lg shadow-md transition w-full
      ${disabled
            ? "bg-blue-300 cursor-not-allowed"
            : "bg-blue-600 hover:bg-blue-700 text-white"}
      ${className}`}
    >
        {children}
    </button>
);

export default Button;
