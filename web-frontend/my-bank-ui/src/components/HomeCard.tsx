import { Link } from "react-router-dom";
import type { ReactNode } from "react";

interface HomeCardProps {
    title: string;
    description: string;
    link: string;
    buttonText: string;
    icon?: ReactNode;
    className?: string;
}

const isExternal = (url: string) => /^https?:\/\//i.test(url);

export default function HomeCard({
                                     title,
                                     description,
                                     link,
                                     buttonText,
                                     icon,
                                     className = "",
                                 }: HomeCardProps) {
    const btnClass =
        "inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg " +
        "bg-indigo-600 text-white hover:bg-indigo-700 active:translate-y-[1px] " +
        "focus:outline-none focus:ring-2 focus:ring-indigo-400 " +
        "focus:ring-offset-2 focus:ring-offset-gray-900 transition";

    return (
        <div
            className={[
                "relative overflow-hidden rounded-xl",
                "bg-gradient-to-br from-gray-700 via-gray-800 to-gray-900",
                "border border-gray-700/60 shadow-[0_8px_30px_rgb(0,0,0,0.12)]",
                "transition-all duration-300",
                "hover:-translate-y-0.5 hover:shadow-[0_12px_40px_rgb(0,0,0,0.25)]",
                className,
            ].join(" ")}
        >
            {/* subtle decorative gradient */}
            <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-0 bg-[radial-gradient(1200px_300px_at_100%_-20%,rgba(99,102,241,0.18),transparent)]"
            />

            <div className="relative p-6 flex flex-col gap-4">
                <div className="flex items-start gap-3">
                    {icon && (
                        <div className="shrink-0 p-2 rounded-lg bg-indigo-500/10 ring-1 ring-indigo-500/30 text-indigo-300">
                            {icon}
                        </div>
                    )}
                    <div>
                        <h3 className="text-lg sm:text-xl font-semibold text-indigo-100">{title}</h3>
                        <p className="mt-1 text-sm text-indigo-200/80">{description}</p>
                    </div>
                </div>

                <div className="pt-2">
                    {isExternal(link) ? (
                        <a
                            href={link}
                            target="_blank"
                            rel="noopener noreferrer"
                            className={btnClass}
                            aria-label={title}
                        >
                            {buttonText}
                            <svg
                                className="w-4 h-4 opacity-90"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                viewBox="0 0 24 24"
                                aria-hidden="true"
                            >
                                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                            </svg>
                        </a>
                    ) : (
                        <Link to={link} className={btnClass} aria-label={title}>
                            {buttonText}
                            <svg
                                className="w-4 h-4 opacity-90"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                viewBox="0 0 24 24"
                                aria-hidden="true"
                            >
                                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                            </svg>
                        </Link>
                    )}
                </div>
            </div>
        </div>
    );
}
