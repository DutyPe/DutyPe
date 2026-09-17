export function isRejectedIdToken(error: unknown): boolean {
  const code = error && typeof error === "object" && "code" in error ? String(error.code) : "";
  return [
    "auth/id-token-expired", "auth/id-token-revoked", "auth/invalid-id-token",
    "auth/argument-error", "auth/user-disabled", "auth/user-not-found"
  ].includes(code);
}