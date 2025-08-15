import { useEffect, useState } from "react";
import { doPasswordsMatch } from "@/utils/validationUtils";

export function useConfirmPasswordMatch(password: string, confirmPassword: string) {
    const [match, setMatch] = useState<boolean | null>(null);

    useEffect(() => {
        if (!confirmPassword) {
            setMatch(null);
            return;
        }

        setMatch(doPasswordsMatch(password, confirmPassword));
    }, [password, confirmPassword]);

    return match;
}
