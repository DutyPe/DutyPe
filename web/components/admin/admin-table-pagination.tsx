"use client";

type PaginationResult<T> = {
  endIndex: number;
  pageRows: T[];
  safePage: number;
  startIndex: number;
  totalPages: number;
};

type AdminTablePaginationProps = {
  itemLabel: string;
  onPageChange: (page: number) => void;
  onPageSizeChange?: (pageSize: number) => void;
  page: number;
  pageSize: number;
  pageSizeOptions?: number[];
  totalItems: number;
};

export const ADMIN_PAGE_SIZE_OPTIONS = [15, 25, 50, 100];

export function paginateRows<T>(rows: T[], page: number, pageSize: number): PaginationResult<T> {
  const totalPages = Math.max(1, Math.ceil(rows.length / pageSize));
  const safePage = Math.min(Math.max(page, 1), totalPages);
  const startIndex = rows.length === 0 ? 0 : (safePage - 1) * pageSize;
  const endIndex = rows.length === 0 ? 0 : Math.min(startIndex + pageSize, rows.length);

  return {
    endIndex,
    pageRows: rows.slice(startIndex, endIndex),
    safePage,
    startIndex,
    totalPages
  };
}

export function AdminTablePagination({
  itemLabel,
  onPageChange,
  onPageSizeChange,
  page,
  pageSize,
  pageSizeOptions = ADMIN_PAGE_SIZE_OPTIONS,
  totalItems
}: AdminTablePaginationProps) {
  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
  const safePage = Math.min(Math.max(page, 1), totalPages);
  const start = totalItems === 0 ? 0 : (safePage - 1) * pageSize + 1;
  const end = totalItems === 0 ? 0 : Math.min(safePage * pageSize, totalItems);

  return (
    <div className="admin-pagination" aria-label={`${itemLabel} pagination`}>
      <div className="admin-pagination-summary">
        Showing {start}-{end} of {totalItems} {itemLabel}
      </div>

      <div className="admin-pagination-controls">
        {onPageSizeChange ? (
          <label className="admin-page-size">
            <span>Rows</span>
            <select
              value={pageSize}
              onChange={(event) => onPageSizeChange(Number(event.target.value))}
            >
              {pageSizeOptions.map((option) => (
                <option key={option} value={option}>
                  {option}
                </option>
              ))}
            </select>
          </label>
        ) : null}

        <button
          type="button"
          className="admin-pagination-button"
          onClick={() => onPageChange(1)}
          disabled={safePage === 1}
        >
          First
        </button>
        <button
          type="button"
          className="admin-pagination-button"
          onClick={() => onPageChange(safePage - 1)}
          disabled={safePage === 1}
        >
          Previous
        </button>
        <span className="admin-pagination-page">
          Page {safePage} of {totalPages}
        </span>
        <button
          type="button"
          className="admin-pagination-button"
          onClick={() => onPageChange(safePage + 1)}
          disabled={safePage === totalPages}
        >
          Next
        </button>
        <button
          type="button"
          className="admin-pagination-button"
          onClick={() => onPageChange(totalPages)}
          disabled={safePage === totalPages}
        >
          Last
        </button>
      </div>
    </div>
  );
}
