"use client";

import {
  ColumnDef,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  SortingState,
  useReactTable,
} from "@tanstack/react-table";
import { useState } from "react";
import { Icon } from "@/components/icons";
import { cn } from "@/lib/utils";

export function DataTable<TData>({
  columns,
  data,
  className,
  mobileVisibleColumnIds,
  onRowClick,
  rowLabel,
}: {
  columns: ColumnDef<TData, any>[];
  data: TData[];
  className?: string;
  mobileVisibleColumnIds?: string[];
  onRowClick?: (row: TData) => void;
  rowLabel?: (row: TData) => string;
}) {
  const [sorting, setSorting] = useState<SortingState>([]);

  const table = useReactTable({
    data,
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  return (
    <div className={cn("overflow-hidden rounded-xl border border-[var(--border-soft)] bg-[var(--surface)]", className)}>
      {!mobileVisibleColumnIds ? (
        <p className="border-b border-[var(--border-soft)] bg-[var(--surface-muted)] px-4 py-2 text-[11px] text-[var(--text-muted)] sm:hidden">
          Таблицу можно прокручивать по горизонтали →
        </p>
      ) : null}
      <div
        aria-label="Таблица данных"
        className="app-scrollbar overflow-auto focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-[var(--accent-strong)]"
        role="region"
        tabIndex={0}
      >
        <table className="min-w-full border-separate border-spacing-0 text-left text-[13px]">
          <thead className="bg-[var(--surface-muted)] font-[var(--font-mono)] text-[10px] uppercase tracking-[0.05em] text-[var(--text-muted)]">
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <th
                    key={header.id}
                    className={cn(
                      "border-b border-[var(--border-soft)] px-4 py-3 font-semibold",
                      mobileVisibleColumnIds && !mobileVisibleColumnIds.includes(header.column.id) ? "hidden sm:table-cell" : "",
                    )}
                  >
                    {header.isPlaceholder ? null : (
                      <button
                        className="inline-flex items-center gap-1 transition hover:text-[var(--text-strong)]"
                        onClick={header.column.getToggleSortingHandler()}
                        type="button"
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {header.column.getIsSorted() ? <Icon className="size-3.5" name="arrow-down-up" /> : null}
                      </button>
                    )}
                  </th>
                ))}
              </tr>
            ))}
          </thead>
          <tbody className="bg-[var(--surface)]">
            {table.getRowModel().rows.map((row) => (
              <tr
                key={row.id}
                className={cn(
                  "min-h-[72px] transition-colors last:border-b-0 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-[var(--accent-strong)]",
                  onRowClick ? "cursor-pointer hover:bg-[var(--background)] focus:bg-[var(--background)]" : "",
                )}
                onClick={() => onRowClick?.(row.original)}
                onKeyDown={(event) => {
                  if (!onRowClick || (event.key !== "Enter" && event.key !== " ")) return;
                  event.preventDefault();
                  onRowClick(row.original);
                }}
                role={onRowClick ? "button" : undefined}
                tabIndex={onRowClick ? 0 : undefined}
                aria-label={onRowClick ? rowLabel?.(row.original) ?? "Открыть строку" : undefined}
              >
                {row.getVisibleCells().map((cell) => (
                  <td
                    key={cell.id}
                    className={cn(
                      "h-[72px] border-b border-[var(--border-soft)] px-4 py-3 align-middle last:border-b-0",
                      mobileVisibleColumnIds && !mobileVisibleColumnIds.includes(cell.column.id) ? "hidden sm:table-cell" : "",
                    )}
                  >
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
