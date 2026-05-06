"use client";

import { useState } from "react";
import { adminApiFetch } from "@/lib/firebase/admin-client-fetch";

type DeleteResponse = {
  ok?: boolean;
  message?: string;
  userId?: string;
  phone?: string;
  deletedCount?: number;
  error?: string;
};

export function DeleteUserByPhoneClient() {
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<DeleteResponse | null>(null);

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPhone(e.target.value);
    setError(null);
    setSuccess(null);
  };

  const handleDelete = async () => {
    const trimmedPhone = phone.trim();

    if (!trimmedPhone) {
      setError("Please enter a phone number.");
      return;
    }

    const confirmed = window.confirm(
      `WARNING: This will permanently delete ALL data associated with:\n\nPhone: ${trimmedPhone}\n\nThis includes:\n- User account\n- Worker profile\n- Employer profile\n- Referral codes\n- All related records\n\nThis action CANNOT be undone.\n\nType the phone number to confirm.`
    );

    if (!confirmed) {
      return;
    }

    const secondConfirm = window.prompt(
      `Confirm deletion by typing the phone number:\n${trimmedPhone}`
    );

    if (secondConfirm?.trim() !== trimmedPhone) {
      setError("Phone number confirmation did not match. Deletion cancelled.");
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setSuccess(null);

      const response = await adminApiFetch("/api/admin/delete-user-by-phone", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ phone: trimmedPhone })
      });

      const payload = (await response.json()) as DeleteResponse;

      if (!response.ok) {
        throw new Error(payload.error || "Failed to delete user.");
      }

      setSuccess(payload);
      setPhone("");
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "Failed to delete user.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="admin-delete-by-phone-container">
      <div className="admin-delete-by-phone-card">
        <div className="admin-delete-by-phone-header">
          <h2>Delete User by Phone Number</h2>
          <p className="admin-delete-by-phone-subtitle">
            For cases where a user mistakenly joined with the wrong role.
            Permanently removes all associated data.
          </p>
        </div>

        <div className="admin-delete-by-phone-form">
          <div className="form-group">
            <label htmlFor="phone-input">Phone Number (E.164 format, e.g., +919xxxxxxxxx):</label>
            <input
              id="phone-input"
              type="text"
              className="admin-input"
              placeholder="+919123456789"
              value={phone}
              onChange={handlePhoneChange}
              disabled={loading}
              maxLength={20}
            />
          </div>

          <button
            onClick={handleDelete}
            disabled={loading || !phone.trim()}
            className="admin-button danger"
            style={{ marginTop: "1rem" }}
          >
            {loading ? "Deleting..." : "Delete All User Data"}
          </button>
        </div>

        {error && (
          <div className="admin-error" style={{ marginTop: "1rem" }}>
            <strong>Error:</strong> {error}
          </div>
        )}

        {success && (
          <div className="admin-success" style={{ marginTop: "1rem" }}>
            <strong>Success!</strong>
            <p>{success.message}</p>
            <div style={{ fontSize: "0.9rem", marginTop: "0.5rem", color: "#666" }}>
              <p><strong>User ID:</strong> <code>{success.userId}</code></p>
              <p><strong>Phone:</strong> <code>{success.phone}</code></p>
              <p><strong>Records deleted:</strong> {success.deletedCount}</p>
            </div>
          </div>
        )}

        <div className="admin-delete-by-phone-info" style={{ marginTop: "2rem" }}>
          <h3>What Gets Deleted</h3>
          <ul>
            <li>User account record</li>
            <li>Worker profile (if exists)</li>
            <li>Employer profile (if exists)</li>
            <li>Phone role mapping</li>
            <li>Referral codes owned by user</li>
            <li>Referral stats</li>
            <li>Firebase Auth user</li>
          </ul>
        </div>

        <div className="admin-delete-by-phone-warning" style={{ marginTop: "1.5rem" }}>
          <strong>Warning:</strong>
          <ul>
            <li>This action is <strong>irreversible</strong></li>
            <li>User cannot login after deletion</li>
            <li>All job postings and applications will become orphaned</li>
            <li>Referral relationships involving this user are NOT deleted</li>
            <li>A log of this deletion is recorded in Firebase audit logs</li>
          </ul>
        </div>
      </div>

      <style jsx>{`
        .admin-delete-by-phone-container {
          max-width: 600px;
          margin: 0 auto;
          padding: 2rem;
        }

        .admin-delete-by-phone-card {
          background: #fff;
          border: 1px solid #e0e0e0;
          border-radius: 8px;
          padding: 2rem;
          box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
        }

        .admin-delete-by-phone-header h2 {
          margin: 0 0 0.5rem 0;
          font-size: 1.5rem;
          color: #333;
        }

        .admin-delete-by-phone-subtitle {
          margin: 0;
          color: #666;
          font-size: 0.95rem;
        }

        .admin-delete-by-phone-form {
          margin-top: 1.5rem;
        }

        .form-group {
          margin-bottom: 1rem;
        }

        .form-group label {
          display: block;
          margin-bottom: 0.5rem;
          font-weight: 500;
          color: #333;
          font-size: 0.95rem;
        }

        .admin-input {
          width: 100%;
          padding: 0.75rem;
          border: 1px solid #ccc;
          border-radius: 4px;
          font-size: 1rem;
          font-family: monospace;
        }

        .admin-input:disabled {
          background-color: #f5f5f5;
          cursor: not-allowed;
        }

        .admin-button.danger {
          background-color: #d32f2f;
          color: white;
          padding: 0.75rem 1.5rem;
          border: none;
          border-radius: 4px;
          font-size: 1rem;
          font-weight: 500;
          cursor: pointer;
          transition: background-color 0.2s;
        }

        .admin-button.danger:hover:not(:disabled) {
          background-color: #b71c1c;
        }

        .admin-button.danger:disabled {
          background-color: #ccc;
          cursor: not-allowed;
        }

        .admin-error {
          padding: 1rem;
          background-color: #ffebee;
          border-left: 4px solid #d32f2f;
          color: #c62828;
          border-radius: 4px;
          font-size: 0.95rem;
        }

        .admin-success {
          padding: 1rem;
          background-color: #e8f5e9;
          border-left: 4px solid #4caf50;
          color: #2e7d32;
          border-radius: 4px;
          font-size: 0.95rem;
        }

        .admin-success code {
          background-color: #c8e6c9;
          padding: 0.2rem 0.4rem;
          border-radius: 2px;
          font-family: monospace;
          word-break: break-all;
        }

        .admin-delete-by-phone-info {
          background-color: #f5f5f5;
          padding: 1rem;
          border-radius: 4px;
        }

        .admin-delete-by-phone-info h3 {
          margin: 0 0 0.5rem 0;
          font-size: 1rem;
          color: #333;
        }

        .admin-delete-by-phone-info ul {
          margin: 0;
          padding-left: 1.5rem;
          color: #555;
          font-size: 0.9rem;
        }

        .admin-delete-by-phone-info li {
          margin: 0.25rem 0;
        }

        .admin-delete-by-phone-warning {
          background-color: #fff3e0;
          padding: 1rem;
          border-radius: 4px;
          border-left: 4px solid #ff9800;
        }

        .admin-delete-by-phone-warning strong {
          color: #e65100;
        }

        .admin-delete-by-phone-warning ul {
          margin: 0.5rem 0 0 0;
          padding-left: 1.5rem;
          color: #555;
          font-size: 0.9rem;
        }

        .admin-delete-by-phone-warning li {
          margin: 0.25rem 0;
        }

        .admin-delete-by-phone-warning strong {
          font-weight: 600;
        }
      `}</style>
    </div>
  );
}
