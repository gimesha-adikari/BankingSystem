import { useEffect, useState } from "react";
import { isValidEmail } from "../utils/validationUtils";

export function useEmailValidation(email: string) {
    const [valid, setValid] = useState<boolean | null>(null);

    useEffect(() => {
        if (!email || email.trim() === "") {
            setValid(null);
            return;
        }

        setValid(isValidEmail(email));
    }, [email]);

    return valid;
}
