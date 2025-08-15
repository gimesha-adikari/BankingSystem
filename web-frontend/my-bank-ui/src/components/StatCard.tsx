import React from "react";

type Tone = "light" | "dark" | "auto";

interface StatCardProps {
    title: string;
    value: string | number;
    color?: string;
    subtitle?: string;
    icon?: React.ReactNode;
    tone?: Tone;
    className?: string;
    loading?: boolean;
}

function cn(...xs: Array<string | false | null | undefined>) {
    return xs.filter(Boolean).join(" ");
}

const StatCard: React.FC<StatCardProps> = ({
                                               title,
                                               value,
                                               color = "text-indigo-700",
                                               subtitle,
                                               icon,
                                               tone = "light",
                                               className = "",
                                               loading = false,
                                           }) => {
    const light =
        "bg-white/90 ring-1 ring-gray-200 text-slate-900 shadow-sm";
    const dark =
        "bg-neutral-900/80 ring-1 ring-neutral-800 text-neutral-100";
    const auto =
        `${light} dark:bg-neutral-900/80 dark:ring-neutral-800 dark:text-neutral-100`;
    const surface = tone === "dark" ? dark : tone === "auto" ? auto : light;

    return (
        <div
            className={cn(
                "relative overflow-hidden rounded-2xl p-5 transition-all duration-200",
                "backdrop-blur supports-[backdrop-filter]:bg-opacity-90",
                "hover:-translate-y-0.5 hover:shadow-md",
                "focus-within:ring-2 focus-within:ring-indigo-500",
                surface,
                className
            )}
            role="group"
            aria-label={`${title} statistic`}
        >
            <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(700px_160px_at_120%_-20%,rgba(99,102,241,0.12),transparent)]" />
            <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-white/0 via-white/0 to-white/5 dark:to-white/0" />
            <div className="flex items-start gap-3">
                {icon && (
                    <div className="shrink-0 p-2 rounded-xl bg-indigo-500/10 ring-1 ring-indigo-500/20 text-indigo-500">
                        {icon}
                    </div>
                )}
                <div className="flex-1 min-w-0">
                    <h3
                        className={cn(
                            "text-sm font-medium truncate",
                            tone === "dark" ? "text-indigo-200" : "text-slate-600",
                            color
                        )}
                        title={title}
                    >
                        {title}
                    </h3>
                    <div className="mt-1">
                        {loading ? (
                            <div className="h-7 w-32 rounded bg-gray-200 dark:bg-neutral-700 animate-pulse" />
                        ) : (
                            <p
                                className={cn(
                                    "text-3xl font-bold tracking-tight",
                                    tone === "dark" ? "text-white" : "text-slate-900"
                                )}
                            >
                                {value}
                            </p>
                        )}
                    </div>
                    {subtitle && (
                        <p
                            className={cn(
                                "mt-1 text-xs",
                                tone === "dark" ? "text-indigo-300/80" : "text-slate-500"
                            )}
                        >
                            {subtitle}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
};

export default StatCard;
