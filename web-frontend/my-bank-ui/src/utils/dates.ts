export function parseDateFlexible(input: unknown): Date | null {
    if (input == null) return null;

    if (input instanceof Date) return isNaN(+input) ? null : input;

    if (typeof input === "number") {
        const d = new Date(input);
        return isNaN(+d) ? null : d;
    }

    if (typeof input === "string") {
        // Try ISO or common formats; add guards for backend values like "2025-08-15T09:31:00Z"
        const trimmed = input.trim();
        if (!trimmed) return null;

        const d = new Date(trimmed);
        if (!isNaN(+d)) return d;

        // Fallback for "yyyy-MM-dd HH:mm:ss" -> turn space into 'T'
        const alt = new Date(trimmed.replace(" ", "T"));
        return isNaN(+alt) ? null : alt;
    }

    return null;
}

export function fmtDateTimeLocal(input: unknown, locale = navigator.language): string {
    const d = parseDateFlexible(input);
    if (!d) return "—";
    return new Intl.DateTimeFormat(locale, {
        year: "numeric", month: "short", day: "2-digit",
        hour: "2-digit", minute: "2-digit"
    }).format(d);
}

export function safeISOString(input: unknown): string {
    const d = parseDateFlexible(input);
    return d ? d.toISOString() : "";
}
