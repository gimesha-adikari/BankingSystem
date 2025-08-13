package com.bankingsystem.mobile.util

/**
 * Represents the result of a password strength check.
 *
 * @property score An integer score indicating the strength of the password. Higher is better.
 * @property issues A list of strings, where each string describes a weakness found in the password.
 */
data class PasswordStrengthResult(val score: Int, val issues: List<String>)

/**
 * Checks the strength of a given password based on a set of predefined criteria.
 *
 * The criteria for a strong password include:
 * - Minimum length of 8 characters.
 * - At least one uppercase letter.
 * - At least one digit.
 * - At least one special character from the set "!@#$%^&*(),.?\":{}|<>".
 *
 * @param password The password string to evaluate.
 * @return A [PasswordStrengthResult] object containing the strength score and a list of any issues found.
 */
fun checkPasswordStrength(password: String): PasswordStrengthResult {
    var score = 0
    val issues = mutableListOf<String>()

    if (password.length >= 8) score++ else issues.add("Password must be at least 8 characters long")
    if (password.any { it.isUpperCase() }) score++ else issues.add("Must include at least one uppercase letter")
    if (password.any { it.isDigit() }) score++ else issues.add("Must include at least one number")
    if (password.any { "!@#$%^&*(),.?\":{}|<>".contains(it) }) score++ else issues.add("Must include at least one special character")

    return PasswordStrengthResult(score, issues)
}

/**
 * Compares two password strings to see if they match.
 *
 * @param password The first password string.
 * @param confirmPassword The second password string to compare against the first.
 * @return `true` if the passwords match, `false` otherwise.
 */
fun doPasswordsMatch(password: String, confirmPassword: String) = password == confirmPassword

/**
 * Validates an email address string using a regular expression.
 * @param email The email string to validate.
 * @return `true` if the email format is valid, `false` otherwise.
 */
fun isValidEmail(email: String) = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)
