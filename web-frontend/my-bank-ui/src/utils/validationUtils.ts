export async function checkUsernameAvailability(username: string): Promise<boolean> {
    if (!username || username.length < 3) return false;

    try {
        const response = await fetch(`/api/v1/auth/available?username=${encodeURIComponent(username)}`);

        if (response.status === 200) {
            return true; // Username available
        } else if (response.status === 409) {
            return false; // Username taken
        } else {
            console.error("Unexpected response status:", response.status);
            return false;
        }
    } catch (error) {
        console.error("Username availability check failed:", error);
        return false;
    }
}


export function checkPasswordStrength(password: string): {
    score: number,
    issues: string[]
} {
    let score = 0;
    const issues = [];

    if (password.length >= 8) {
        score += 1;
    } else {
        issues.push("Password must be at least 8 characters long");
    }

    if (/[A-Z]/.test(password)) {
        score += 1;
    } else {
        issues.push("Must include at least one uppercase letter");
    }

    if (/[0-9]/.test(password)) {
        score += 1;
    } else {
        issues.push("Must include at least one number");
    }

    if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) {
        score += 1;
    } else {
        issues.push("Must include at least one special character");
    }

    return { score, issues };
}

export function doPasswordsMatch(password: string, confirmPassword: string): boolean {
    return password === confirmPassword;
}

export function isValidEmail(email: string): boolean {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

