import React from "react";
import { cn } from "@/utils/cn";

type Column<T> = {
    header: string;
    accessor: keyof T | ((row: T) => React.ReactNode);
    width?: string;
    align?: "left" | "center" | "right";
};

type Props<T> = {
    data: T[];
    columns: Column<T>[];
    onRowClick?: (row: T) => void;
    rowKey?: (row: T, index: number) => React.Key;
    loading?: boolean;
    emptyMessage?: React.ReactNode;
    tone?: "light" | "dark" | "auto";
    dense?: boolean;
    stickyHeader?: boolean;
    className?: string;
};

function Table<T>({
                      data,
                      columns,
                      onRowClick,
                      rowKey,
                      loading = false,
                      emptyMessage = "No data found.",
                      tone = "light",
                      dense = false,
                      stickyHeader = true,
                      className = "",
                  }: Props<T>) {
    const surface =
        tone === "dark"
            ? "bg-neutral-900/70 ring-1 ring-neutral-800 text-neutral-100"
            : tone === "auto"
                ? "bg-white ring-1 ring-gray-200 text-slate-900 dark:bg-neutral-900/70 dark:ring-neutral-800 dark:text-neutral-100"
                : "bg-white ring-1 ring-gray-200 text-slate-900";

    const headerSurface =
        tone === "dark"
            ? "bg-neutral-900/95 text-indigo-100"
            : tone === "auto"
                ? "bg-slate-50 text-slate-600 dark:bg-neutral-900/95 dark:text-indigo-100"
                : "bg-slate-50 text-slate-600";

    const rowBase =
        tone === "dark"
            ? "text-neutral-100 border-neutral-800"
            : tone === "auto"
                ? "text-slate-800 border-gray-200 dark:text-neutral-100 dark:border-neutral-800"
                : "text-slate-800 border-gray-200";

    const zebraBg =
        tone === "dark"
            ? "odd:bg-neutral-900/40 even:bg-neutral-900/60"
            : tone === "auto"
                ? "odd:bg-white even:bg-slate-50 dark:odd:bg-neutral-900/40 dark:even:bg-neutral-900/60"
                : "odd:bg-white even:bg-slate-50";

    const hoverBg =
        tone === "dark"
            ? "hover:bg-neutral-800/70"
            : tone === "auto"
                ? "hover:bg-slate-100 dark:hover:bg-neutral-800/70"
                : "hover:bg-slate-100";

    const cellPad = dense ? "px-3 py-2" : "px-5 py-3";

    const handleKeyRow =
        onRowClick &&
        ((e: React.KeyboardEvent<HTMLTableRowElement>, row: T) => {
            if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                onRowClick(row);
            }
        });

    return (
        <div className={cn("overflow-x-auto rounded-2xl", className)}>
            <div className={cn("rounded-2xl overflow-hidden backdrop-blur", surface)}>
                <table className="min-w-full text-sm">
                    <thead
                        className={cn(
                            stickyHeader && "sticky top-0 z-10",
                            headerSurface,
                            "backdrop-blur supports-[backdrop-filter]:bg-opacity-95"
                        )}
                    >
                    <tr className="border-b border-black/5 dark:border-white/10">
                        {columns.map((col, idx) => (
                            <th
                                key={idx}
                                className={cn(
                                    cellPad,
                                    "font-medium uppercase tracking-wide text-[11px]",
                                    col.align === "center"
                                        ? "text-center"
                                        : col.align === "right"
                                            ? "text-right"
                                            : "text-left"
                                )}
                                style={{ width: col.width }}
                                scope="col"
                            >
                                {col.header}
                            </th>
                        ))}
                    </tr>
                    </thead>

                    {loading ? (
                        <tbody>
                        {Array.from({ length: 6 }).map((_, r) => (
                            <tr key={`skeleton-${r}`} className={cn(rowBase, zebraBg)}>
                                {columns.map((_, c) => (
                                    <td key={c} className={cn(cellPad, "border-t")}>
                                        <div className="h-4 w-[70%] max-w-[240px] rounded bg-gray-200 dark:bg-neutral-700 animate-pulse" />
                                    </td>
                                ))}
                            </tr>
                        ))}
                        </tbody>
                    ) : data.length === 0 ? (
                        <tbody>
                        <tr className={rowBase}>
                            <td colSpan={columns.length} className={cn(cellPad, "text-center italic")}>
                                {emptyMessage}
                            </td>
                        </tr>
                        </tbody>
                    ) : (
                        <tbody>
                        {data.map((row, idx) => {
                            const k = rowKey ? rowKey(row, idx) : idx;
                            const clickable = !!onRowClick;
                            return (
                                <tr
                                    key={k}
                                    className={cn(
                                        rowBase,
                                        zebraBg,
                                        "border-t transition",
                                        clickable &&
                                        "cursor-pointer focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 rounded-md",
                                        hoverBg
                                    )}
                                    onClick={clickable ? () => onRowClick!(row) : undefined}
                                    tabIndex={clickable ? 0 : -1}
                                    onKeyDown={clickable ? (e) => handleKeyRow!(e, row) : undefined}
                                    role={clickable ? "button" : undefined}
                                    aria-label={clickable ? "table row, clickable" : undefined}
                                >
                                    {columns.map((col, cIdx) => {
                                        const cell =
                                            typeof col.accessor === "function"
                                                ? col.accessor(row)
                                                : (row[col.accessor] as React.ReactNode);

                                        return (
                                            <td
                                                key={cIdx}
                                                className={cn(
                                                    cellPad,
                                                    col.align === "center"
                                                        ? "text-center"
                                                        : col.align === "right"
                                                            ? "text-right tabular-nums"
                                                            : "text-left"
                                                )}
                                            >
                                                {cell}
                                            </td>
                                        );
                                    })}
                                </tr>
                            );
                        })}
                        </tbody>
                    )}
                </table>
            </div>
        </div>
    );
}

export default Table;
