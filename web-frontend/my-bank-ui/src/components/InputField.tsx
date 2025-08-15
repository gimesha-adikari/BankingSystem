import React, { forwardRef, useId, useState } from "react";

type Tone = "light" | "dark" | "auto";

type Props = Omit<
    React.InputHTMLAttributes<HTMLInputElement>,
    "name" | "value" | "onChange" | "type" | "placeholder"
> & {
    label: string;
    name: string;
    value: string | number;
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
    type?: React.HTMLInputTypeAttribute;
    placeholder?: string;
    error?: string;
    hint?: React.ReactNode;
    leftIcon?: React.ReactNode;
    rightIcon?: React.ReactNode;
    tone?: Tone;
    containerClassName?: string;
    className?: string;
};

const InputField = forwardRef<HTMLInputElement, Props>(
    (
        {
            label,
            name,
            value,
            onChange,
            type = "text",
            placeholder,
            error,
            hint,
            disabled = false,
            required = false,
            leftIcon,
            rightIcon,
            tone = "light",
            containerClassName = "",
            className = "",
            ...rest
        },
        ref
    ) => {
        const rid = useId();
        const inputId = `${name}-${rid}`;

        const [showPassword, setShowPassword] = useState(false);
        const isPassword = type === "password";
        const actualType = isPassword && showPassword ? "text" : type;

        const base =
            "w-full rounded-xl px-3.5 py-2.5 text-sm leading-5 transition outline-none placeholder:font-normal placeholder:opacity-80";

        const light =
            "bg-white text-gray-900 placeholder:text-gray-500 ring-1 ring-gray-300 focus:ring-2 focus:ring-indigo-500";
        const dark =
            "bg-neutral-900 text-neutral-100 placeholder:text-neutral-400 ring-1 ring-neutral-700 focus:ring-2 focus:ring-indigo-500";
        const auto =
            `${light} dark:bg-neutral-900 dark:text-neutral-100 dark:placeholder:text-neutral-400 dark:ring-neutral-700`;

        const palette = tone === "dark" ? dark : tone === "auto" ? auto : light;

        const invalid = error
            ? "ring-1 ring-rose-500 focus:ring-2 focus:ring-rose-500"
            : "";
        const disabledCls = disabled
            ? "opacity-60 cursor-not-allowed bg-gray-100 dark:bg-neutral-800"
            : "";

        const withLeft = leftIcon ? "pl-10" : "";
        const withRight = isPassword || rightIcon ? "pr-10" : "";

        const describedIds = [
            error ? `${inputId}-error` : null,
            hint ? `${inputId}-hint` : null,
        ]
            .filter(Boolean)
            .join(" ") || undefined;

        return (
            <div className={`relative ${containerClassName}`}>
                {/* label */}
                <label
                    htmlFor={inputId}
                    className="block text-sm font-medium mb-1 text-gray-800 dark:text-gray-200"
                >
                    {label}{" "}
                    {required && <span className="text-rose-600 dark:text-rose-400">*</span>}
                </label>

                <div className="relative">
                    {/* left icon */}
                    {leftIcon && (
                        <span
                            className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 dark:text-neutral-400 pointer-events-none"
                            aria-hidden="true"
                        >
              {leftIcon}
            </span>
                    )}

                    {/* input */}
                    <input
                        id={inputId}
                        ref={ref}
                        type={actualType}
                        name={name}
                        value={value}
                        onChange={onChange}
                        placeholder={placeholder}
                        disabled={disabled}
                        aria-invalid={!!error || undefined}
                        aria-describedby={describedIds}
                        aria-required={required || undefined}
                        data-invalid={!!error || undefined}
                        className={`${base} ${palette} ${invalid} ${disabledCls} ${withLeft} ${withRight} ${className}`}
                        {...rest}
                    />

                    {/* right icon / password toggle */}
                    <div className="absolute right-2 top-1/2 -translate-y-1/2">
                        {isPassword ? (
                            <button
                                type="button"
                                onClick={() => setShowPassword((s) => !s)}
                                className="
                  p-1.5 rounded-md
                  text-gray-500 dark:text-neutral-400
                  hover:text-gray-800 dark:hover:text-neutral-100
                  hover:bg-black/5 dark:hover:bg-white/10
                  focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500
                "
                                aria-label={showPassword ? "Hide password" : "Show password"}
                            >
                                {showPassword ? (
                                    // eye-off
                                    <svg
                                        xmlns="http://www.w3.org/2000/svg"
                                        className="h-5 w-5"
                                        fill="none"
                                        viewBox="0 0 24 24"
                                        stroke="currentColor"
                                        strokeWidth={2}
                                        aria-hidden="true"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            d="M3 3l18 18M10.477 10.477A3 3 0 0012 15a3 3 0 002.523-4.523M6.6 6.6A10.05 10.05 0 002 12c1.5 3.667 5.5 6 10 6 1.112 0 2.186-.152 3.2-.438M17.4 17.4A10.04 10.04 0 0022 12c-1.5-3.667-5.5-6-10-6"
                                        />
                                    </svg>
                                ) : (
                                    // eye
                                    <svg
                                        xmlns="http://www.w3.org/2000/svg"
                                        className="h-5 w-5"
                                        fill="none"
                                        viewBox="0 0 24 24"
                                        stroke="currentColor"
                                        strokeWidth={2}
                                        aria-hidden="true"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                                        />
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            d="M2.458 12C3.732 7.943 7.523 5 12 5c4.477 0 8.268 2.943 9.542 7-1.274 4.057-5.065 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
                                        />
                                    </svg>
                                )}
                            </button>
                        ) : rightIcon ? (
                            <span className="text-gray-400 dark:text-neutral-400" aria-hidden="true">
                {rightIcon}
              </span>
                        ) : null}
                    </div>
                </div>

                {/* help / error */}
                {error ? (
                    <p id={`${inputId}-error`} className="mt-1 text-sm text-rose-600 dark:text-rose-400">
                        {error}
                    </p>
                ) : hint ? (
                    <p id={`${inputId}-hint`} className="mt-1 text-xs text-gray-600 dark:text-neutral-400">
                        {hint}
                    </p>
                ) : null}
            </div>
        );
    }
);

InputField.displayName = "InputField";
export default InputField;
