import { useEffect, useState } from "react";
import { checkUsernameAvailability } from "@/utils/validationUtils";

export function useUsernameAvailability(username: string) {
    const [available, setAvailable] = useState<boolean | null>(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (!username || username.trim().length < 3) {
            setAvailable(null);
            return;
        }

        setLoading(true);
        const timeoutId = setTimeout(() => {
            checkUsernameAvailability(username)
                .then((result) => {
                    setAvailable(result);
                })
                .catch((error) => {
                    console.error("Username availability check failed:", error);
                    setAvailable(null);
                })
                .finally(() => {
                    setLoading(false);
                });
        }, 500);

        return () => {
            clearTimeout(timeoutId);
        };
    }, [username]);

    return { available, loading };
}
