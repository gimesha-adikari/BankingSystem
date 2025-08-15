import React, { useEffect, useMemo, useRef } from "react";
import { useAlert } from "@/contexts/use-alert";
import { CheckCircle2, XCircle, Info, AlertTriangle, X } from "lucide-react";

const AUTO_DISMISS_MS = 4000;

type Variant = "success" | "error" | "warning" | "info";

const Alert: React.FC = () => {
    const { message, type, visible, hideAlert } = useAlert();
    const barRef = useRef<HTMLDivElement | null>(null);
    const timerRef = useRef<number | null>(null);
    const pausedRef = useRef(false);

    const style = useMemo(() => {
        const base =
            "relative flex items-start gap-3 rounded-2xl border ring-1 ring-black/5 dark:ring-white/10 shadow-2xl shadow-black/10 px-4 py-3 overflow-hidden";
        const glass =
            "bg-white/85 supports-[backdrop-filter]:bg-white/70 dark:bg-neutral-900/80 backdrop-blur";
        const text = "text-gray-900 dark:text-gray-100";
        const iconWrap =
            "inline-flex items-center justify-center rounded-xl ring-1 ring-black/5 dark:ring-white/10 p-1.5";
        const closeBtn =
            "shrink-0 p-1 rounded-md transition outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 hover:bg-black/5 dark:hover:bg-white/10 text-gray-500 hover:text-gray-800 dark:hover:text-gray-100";

        const map: Record<Variant, {
            container: string;
            iconEl: React.ReactNode;
            badge: string;
            bar: string;
            accent: string;
            role: "alert" | "status";
        }> = {
            success: {
                container: `${base} ${glass} ${text} border-emerald-600/20`,
                iconEl: (
                    <span className={`${iconWrap} bg-emerald-500/10`}>
            <CheckCircle2 className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
          </span>
                ),
                badge:
                    "bg-gradient-to-br from-emerald-400/20 via-transparent to-transparent",
                bar: "bg-emerald-500",
                accent: "from-emerald-500/15",
                role: "status",
            },
            error: {
                container: `${base} ${glass} ${text} border-rose-600/25`,
                iconEl: (
                    <span className={`${iconWrap} bg-rose-500/10`}>
            <XCircle className="w-5 h-5 text-rose-600 dark:text-rose-400" />
          </span>
                ),
                badge:
                    "bg-gradient-to-br from-rose-400/20 via-transparent to-transparent",
                bar: "bg-rose-500",
                accent: "from-rose-500/15",
                role: "alert",
            },
            warning: {
                container: `${base} ${glass} ${text} border-amber-600/25`,
                iconEl: (
                    <span className={`${iconWrap} bg-amber-500/10`}>
            <AlertTriangle className="w-5 h-5 text-amber-600 dark:text-amber-400" />
          </span>
                ),
                badge:
                    "bg-gradient-to-br from-amber-400/20 via-transparent to-transparent",
                bar: "bg-amber-500",
                accent: "from-amber-500/15",
                role: "alert",
            },
            info: {
                container: `${base} ${glass} ${text} border-sky-600/20`,
                iconEl: (
                    <span className={`${iconWrap} bg-sky-500/10`}>
            <Info className="w-5 h-5 text-sky-600 dark:text-sky-400" />
          </span>
                ),
                badge:
                    "bg-gradient-to-br from-sky-400/20 via-transparent to-transparent",
                bar: "bg-sky-500",
                accent: "from-sky-500/15",
                role: "status",
            },
        };

        const key = (type as Variant) || "info";
        return {
            ...map[key],
            closeBtn,
            textClasses: "text-sm leading-5",
        };
    }, [type]);

    useEffect(() => {
        if (!visible) return;

        const clear = () => {
            if (timerRef.current) {
                window.clearTimeout(timerRef.current);
                timerRef.current = null;
            }
            if (barRef.current) {
                barRef.current.style.transition = "none";
            }
        };

        const start = () => {
            clear();
            timerRef.current = window.setTimeout(() => {
                if (!pausedRef.current) hideAlert();
            }, AUTO_DISMISS_MS);

            if (barRef.current) {
                barRef.current.style.transition = "none";
                barRef.current.style.transform = "scaleX(1)";
                // eslint-disable-next-line @typescript-eslint/no-unused-expressions
                barRef.current.offsetHeight;
                barRef.current.style.transition = `transform ${AUTO_DISMISS_MS}ms linear`;
                barRef.current.style.transform = "scaleX(0)";
            }
        };

        start();
        return clear;
    }, [visible, hideAlert]);

    useEffect(() => {
        if (!visible) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === "Escape") hideAlert();
        };
        window.addEventListener("keydown", onKey);
        return () => window.removeEventListener("keydown", onKey);
    }, [visible, hideAlert]);

    if (!visible) return null;

    const onMouseEnter = () => {
        pausedRef.current = true;
    };
    const onMouseLeave = () => {
        pausedRef.current = false;
    };
    const onFocus = () => {
        pausedRef.current = true;
    };
    const onBlur = () => {
        pausedRef.current = false;
    };

    return (
        <div
            className={`
        fixed top-5 right-5 z-[1000] min-w-[260px] max-w-sm
        motion-safe:animate-[fadeSlideIn_.22s_ease-out]
        motion-reduce:animate-none
      `}
            role={style.role}
            aria-live={style.role === "alert" ? "assertive" : "polite"}
            onMouseEnter={onMouseEnter}
            onMouseLeave={onMouseLeave}
            onFocus={onFocus}
            onBlur={onBlur}
        >
            <div className={style.container}>
                {/* soft corner glow / accent */}
                <div
                    aria-hidden="true"
                    className={`pointer-events-none absolute inset-0 ${style.badge}`}
                />

                {/* subtle diagonal sheen */}
                <div
                    aria-hidden="true"
                    className={`pointer-events-none absolute inset-0 bg-gradient-to-tr ${style.accent} to-transparent`}
                />

                {/* Icon */}
                <div className="pt-0.5 shrink-0">{style.iconEl}</div>

                {/* Message */}
                <div className="flex-1">
                    {typeof message === "string" ? (
                        <p className={`${style.textClasses} whitespace-pre-line`}>
                            {message}
                        </p>
                    ) : (
                        message
                    )}
                </div>

                {/* Close */}
                <button
                    onClick={hideAlert}
                    className={style.closeBtn}
                    aria-label="Dismiss notification"
                >
                    <X className="w-4 h-4" />
                </button>

                {/* Progress bar */}
                <div className="absolute left-0 right-0 bottom-0 h-0.5 bg-black/10 dark:bg-white/10">
                    <div
                        ref={barRef}
                        className={`h-full origin-left ${style.bar}`}
                        style={{ transform: "scaleX(1)" }}
                    />
                </div>
            </div>

            {/* Keyframes */}
            <style>
                {`
        @keyframes fadeSlideIn {
          from { opacity: 0; transform: translateY(-6px); }
          to   { opacity: 1; transform: translateY(0); }
        }
      `}
            </style>
        </div>
    );
};

export default Alert;
