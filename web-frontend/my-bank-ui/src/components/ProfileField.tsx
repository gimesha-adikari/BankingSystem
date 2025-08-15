import React, { useState } from "react";

interface ProfileFieldProps {
    label: string;
    value?: string | null;

    copyable?: boolean;
    icon?: React.ReactNode;
    lines?: 1 | 2 | 3;
    skeleton?: boolean;
    className?: string;
    labelClassName?: string;
    valueClassName?: string;
}

const clampStyle = (lines: number) =>
    lines === 1
        ? "truncate"
        : [
            "overflow-hidden text-ellipsis",
            "[display:-webkit-box]",
            `[-webkit-line-clamp:${lines}]`,
            "[-webkit-box-orient:vertical]",
        ].join(" ");

const ProfileField: React.FC<ProfileFieldProps> = ({
                                                       label,
                                                       value,
                                                       copyable = false,
                                                       icon,
                                                       lines = 1,
                                                       skeleton = false,
                                                       className = "",
                                                       labelClassName = "",
                                                       valueClassName = "",
                                                   }) => {
    const hasValue = !!value?.toString().trim();
    const [copied, setCopied] = useState(false);

    const onCopy = async () => {
        if (!hasValue) return;
        const text = String(value);
        try {
            await navigator.clipboard.writeText(text);
        } catch {
            const ta = document.createElement("textarea");
            ta.value = text;
            document.body.appendChild(ta);
            ta.select();
            document.execCommand("copy");
            document.body.removeChild(ta);
        }
        setCopied(true);
        window.setTimeout(() => setCopied(false), 1200);
    };

    return (
        <div
            className={`
        group rounded-2xl border ring-1 ring-black/5 dark:ring-white/10
        border-gray-200 dark:border-neutral-800
        bg-white/90 dark:bg-neutral-900/80 backdrop-blur
        p-4 transition
        hover:shadow-sm hover:bg-white dark:hover:bg-neutral-900
        ${className}
      `}
        >
            <div className="flex items-start gap-3">
                {icon && (
                    <div
                        className="mt-0.5 text-gray-500 dark:text-neutral-400 shrink-0"
                        aria-hidden="true"
                    >
                        {icon}
                    </div>
                )}

                <div className="flex-1 min-w-0">
                    <p
                        className={`text-xs font-medium uppercase tracking-wide text-gray-600 dark:text-neutral-400 ${labelClassName}`}
                    >
                        {label}
                    </p>

                    {skeleton ? (
                        <div className="mt-2 space-y-2">
                            <div className="h-4 w-2/3 rounded bg-gray-200 dark:bg-neutral-700 animate-pulse" />
                        </div>
                    ) : hasValue ? (
                        <p
                            className={`mt-1 text-base font-medium text-gray-900 dark:text-neutral-100 ${clampStyle(
                                lines
                            )} ${valueClassName}`}
                            title={String(value)}
                        >
                            {value}
                        </p>
                    ) : (
                        <p className="mt-1 text-base italic text-gray-400 dark:text-neutral-500">
                            Not provided
                        </p>
                    )}

                    {/* screen-reader copy feedback */}
                    <span className="sr-only" aria-live="polite">
            {copied ? `${label} copied to clipboard` : ""}
          </span>
                </div>

                {copyable && hasValue && (
                    <button
                        onClick={onCopy}
                        className={`
              mt-5 ml-2 shrink-0 rounded-md px-2.5 py-1.5 text-xs
              text-gray-700 bg-gray-100 ring-1 ring-gray-200
              hover:bg-gray-200 hover:text-gray-900
              focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500
              dark:text-neutral-100 dark:bg-neutral-800 dark:ring-neutral-700
              dark:hover:bg-neutral-700 transition
            `}
                        aria-label={`Copy ${label}`}
                        title={copied ? "Copied!" : "Copy"}
                    >
                        {copied ? "Copied" : "Copy"}
                    </button>
                )}
            </div>
        </div>
    );
};

export default ProfileField;
