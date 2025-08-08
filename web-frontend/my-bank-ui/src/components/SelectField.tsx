import React from "react";

type Option = {
    value: string;
    label: string;
};

type Props = {
    label: string;
    name: string;
    value: string;
    onChange: (e: React.ChangeEvent<HTMLSelectElement>) => void;
    options: Option[];
    error?: string;
};

const SelectField = ({ label, name, value, onChange, options, error }: Props) => {
    return (
        <div className="relative">
            <label htmlFor={name} className="block text-sm font-medium text-gray-700 mb-1">
                {label}
            </label>
            <select
                id={name}
                name={name}
                value={value}
                onChange={onChange}
                className={`w-full border rounded-lg px-4 py-2 focus:ring-2 focus:outline-none transition
          ${error ? "border-red-500 focus:ring-red-500" : "border-gray-300 focus:ring-blue-500"}`}
            >
                {options.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                        {opt.label}
                    </option>
                ))}
            </select>
            {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
        </div>
    );
};

export default SelectField;
