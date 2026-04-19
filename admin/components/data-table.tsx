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
import { cn } from "@/lib/utils";

export function DataTable<TData>({
  columns,
  data,
  className,
  onRowClick,
}: {
  columns: ColumnDef<TData, any>[];
  data: TData[];
  className?: string;
  onRowClick?: (row: TData) => void;
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
    <div className={cn("overflow-hidden rounded-[var(--radius)] border border-[var(--border)]", className)}>
      <div className="overflow-auto">
        <table className="min-w-full border-separate border-spacing-0 text-left text-sm">
          <thead className="bg-[var(--surface-muted)] text-[var(--text-muted)]">
            {table.getHeaderGroups().map((headerGroup) => (
              <tr key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <th key={header.id} className="border-b border-[var(--border)] px-4 py-3 font-medium">
                    {header.isPlaceholder ? null : (
                      <button
                        className="inline-flex items-center gap-1 transition hover:text-[var(--text-strong)]"
                        onClick={header.column.getToggleSortingHandler()}
                        type="button"
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {{
                          asc: "↑",
                          desc: "↓",
                        }[header.column.getIsSorted() as string] ?? null}
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
                  "border-b border-[var(--border)] transition last:border-b-0",
                  onRowClick ? "cursor-pointer hover:bg-[var(--surface-muted)]" : "",
                )}
                onClick={() => onRowClick?.(row.original)}
              >
                {row.getVisibleCells().map((cell) => (
                  <td key={cell.id} className="border-b border-[var(--border)] px-4 py-3 align-top last:border-b-0">
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
