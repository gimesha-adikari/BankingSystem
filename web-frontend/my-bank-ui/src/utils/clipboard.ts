export type CopyResult = "copied" | "prompt" | "unsupported";

/**
 * Copies text using the Async Clipboard API.
 * Falls back to a prompt (no deprecated execCommand).
 */
export async function copyTextToClipboard(text: string): Promise<CopyResult> {
    if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
        try {
            await navigator.clipboard.writeText(text);
            return "copied";
        } catch {
            // continue to fallback
        }
    }

    try {
        window.prompt("Copy this ID and press Enter:", text);
        return "prompt";
    } catch {
        return "unsupported";
    }
}
