import { useEffect, useState } from "react";
import { checkPasswordStrength } from "@/utils/validationUtils";

export function usePasswordStrength(password: string) {
    const [score, setScore] = useState(0);
    const [issues, setIssues] = useState<string[]>([]);

    useEffect(() => {
        const result = checkPasswordStrength(password);
        setScore(result.score);
        setIssues(result.issues);
    }, [password]);

    return { score, issues };
}
