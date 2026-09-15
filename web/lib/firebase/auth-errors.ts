export const FIREBASE_SETUP_ERROR =
  "Account access is unavailable because this website's Firebase configuration is incomplete. Please contact support.";

export function firebaseAuthErrorMessage(error: unknown): string {
  const code = error && typeof error === "object" && "code" in error
    ? String(error.code).toLowerCase().replace(/_/g, "-")
    : "";

  if (code.includes("api-key") || code === "auth/app-not-authorized" || code === "auth/invalid-app-id") {
    return "The account service rejected this website's Firebase configuration. Please contact support.";
  }
  if (code === "auth/unauthorized-domain") {
    return "Account access is not enabled for this website address. Please contact support.";
  }
  if (code === "auth/operation-not-allowed") {
    return "This sign-in method is not enabled for this website. Please contact support.";
  }
  if (code === "app/account-timeout") {
    return "Account loading timed out. Check your connection and try again.";
  }
  if (["auth/invalid-credential", "auth/user-not-found", "auth/wrong-password"].includes(code)) {
    return "The email or password is incorrect.";
  }
  if (code === "auth/invalid-email") return "Enter a valid email address.";
  if (code === "auth/weak-password") return "Use a password with at least 6 characters.";
  if (code === "auth/email-already-in-use") return "This email already has an account. Sign in or reset your password.";
  if (code === "auth/network-request-failed") return "Unable to reach the account service. Check your connection and try again.";
  if (code === "auth/too-many-requests") return "Too many attempts. Please try again later.";
  if (code === "auth/invalid-phone-number") return "Enter a valid mobile number with its country code.";
  if (code === "auth/invalid-verification-code") return "That code is incorrect. Check the SMS and try again.";
  if (["auth/code-expired", "auth/session-expired"].includes(code)) return "This code has expired. Request a new verification code.";
  if (["auth/captcha-check-failed", "auth/missing-app-credential", "auth/invalid-app-credential"].includes(code)) return "Verification could not be completed. Please try sending the code again.";
  if (code === "auth/quota-exceeded") return "SMS verification is temporarily unavailable. Try Google sign-in or try again later.";
  if (code === "auth/popup-blocked") return "Allow the Google sign-in popup in your browser, then try again.";
  if (code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request") return "Google sign-in was cancelled. You can try again.";
  if (code === "auth/account-exists-with-different-credential") return "Use your existing sign-in method for this email. Accounts are not linked automatically.";
  return "Unable to access your account right now. Please try again.";
}