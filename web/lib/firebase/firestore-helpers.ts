type FirestoreTimestampLike = {
  seconds?: number;
  _seconds?: number;
  toDate?: () => Date;
};

export function readTimestamp(value: unknown): Date | null {
  if (!value) {
    return null;
  }

  if (value instanceof Date) {
    return value;
  }

  if (typeof value === "number") {
    return new Date(value);
  }

  if (typeof value === "string") {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }

  if (typeof value === "object") {
    const candidate = value as FirestoreTimestampLike;

    if (typeof candidate.toDate === "function") {
      return candidate.toDate();
    }

    const seconds = typeof candidate.seconds === "number" ? candidate.seconds : candidate._seconds;
    if (typeof seconds === "number") {
      return new Date(seconds * 1000);
    }
  }

  return null;
}

export function formatDateTime(value: unknown): string {
  const parsed = readTimestamp(value);
  if (!parsed) {
    return "N/A";
  }

  return parsed.toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "medium"
  });
}

export function formatDate(value: unknown): string {
  const parsed = readTimestamp(value);
  if (!parsed) {
    return "N/A";
  }

  return parsed.toLocaleDateString("en-IN", {
    dateStyle: "medium"
  });
}

export function formatCurrencyRange(amount: unknown, payType: unknown): string {
  const unit = typeof payType === "string" && payType ? ` / ${payType.toLowerCase()}` : "";

  if (typeof amount === "string") {
    const raw = amount.trim();
    if (!raw) {
      return "Salary not available";
    }

    const cleaned = raw.replace(/[\u20b9,]/g, "").trim();
    const range = cleaned.match(/^(\d+(?:\.\d+)?)\s*[-\u2013]\s*(\d+(?:\.\d+)?)$/);
    if (range) {
      const min = Number(range[1]);
      const max = Number(range[2]);
      return `Rs ${min.toLocaleString("en-IN")} - Rs ${max.toLocaleString("en-IN")}${unit}`;
    }

    const numericString = Number(cleaned);
    if (Number.isFinite(numericString) && numericString > 0) {
      return `Rs ${numericString.toLocaleString("en-IN")}${unit}`;
    }

    return raw;
  }

  const numeric = typeof amount === "number" ? amount : Number(amount ?? 0);

  if (!numeric) {
    return "Salary not available";
  }

  return `Rs ${numeric.toLocaleString("en-IN")}${unit}`;
}

export function formatCurrency(amount: unknown): string {
  const numeric = typeof amount === "number" ? amount : Number(amount ?? 0);

  if (!numeric) {
    return "Rs 0";
  }

  return `Rs ${numeric.toLocaleString("en-IN")}`;
}

export function formatTextList(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.map(String).map((item) => item.trim()).filter(Boolean);
  }

  if (typeof value === "string") {
    return value
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);
  }

  return [];
}
