import React from "react";

type Column<T> = {
    header: string;
    accessor: keyof T | ((row: T) => React.ReactNode);
    width?: string; // optional width styling
};

type Props<T> = {
    data: T[];
    columns: Column<T>[];
    onRowClick?: (row: T) => void;
};

function Table<T>({ data, columns, onRowClick }: Props<T>) {
    return (
        <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 border border-gray-300 rounded-lg">
                <thead className="bg-gray-100">
                <tr>
                    {columns.map((col, idx) => (
                        <th
                            key={idx}
                            className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                            style={{ width: col.width }}
                        >
                            {col.header}
                        </th>
                    ))}
                </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                {data.length === 0 ? (
                    <tr>
                        <td colSpan={columns.length} className="px-6 py-4 text-center text-gray-500 italic">
                            No data found.
                        </td>
                    </tr>
                ) : (
                    data.map((row, idx) => (
                        <tr
                            key={idx}
                            onClick={() => onRowClick && onRowClick(row)}
                            className={`cursor-pointer hover:bg-gray-100 transition ${
                                idx % 2 === 0 ? "" : "bg-gray-50"
                            }`}
                        >
                            {columns.map((col, cIdx) => {
                                const cell =
                                    typeof col.accessor === "function"
                                        ? col.accessor(row)
                                        : (row[col.accessor] as React.ReactNode);
                                return (
                                    <td key={cIdx} className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                                        {cell}
                                    </td>
                                );
                            })}
                        </tr>
                    ))
                )}
                </tbody>
            </table>
        </div>
    );
}

export default Table;
