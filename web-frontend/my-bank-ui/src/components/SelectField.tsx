import React, { forwardRef, useId } from "react";
import type { ReactNode } from "react";
import * as Select from "@radix-ui/react-select";
import { Check, ChevronDown } from "lucide-react";

type Tone = "light" | "dark" | "auto";
type Option = { value: string | number; label: string; disabled?: boolean };

type Props = {
    label: string;
    name: string;
    value?: string | number | null;
    onChange: (e: { target: { name: string; value: string } }) => void;
    options: Option[];
    error?: string;
    hint?: ReactNode;
    placeholder?: string;
    tone?: Tone;
    disabled?: boolean;
    required?: boolean;
    containerClassName?: string;
    className?: string;
};

const AnimatedSelectField = forwardRef<HTMLButtonElement, Props>(
    (
        {
            label,
            name,
            value,
            onChange,
            options,
            error,
            hint,
            placeholder = "Select an option",
            tone = "light",
            disabled = false,
            required = false,
            containerClassName = "",
            className = "",
        },
        ref
    ) => {
        const rid = useId();
        const selectId = `${name}-${rid}`;

        const baseTrigger =
            "w-full inline-flex items-center justify-between rounded-2xl px-3.5 py-2.5 text-sm leading-5 transition outline-none";
        const light =
            "bg-white text-gray-900 ring-1 ring-gray-300 focus:ring-2 focus:ring-indigo-500";
        const dark =
            "bg-neutral-900 text-neutral-100 ring-1 ring-neutral-700 focus:ring-2 focus:ring-indigo-500";
        const auto =
            `${light} dark:bg-neutral-900 dark:text-neutral-100 dark:ring-neutral-700`;
        const palette = tone === "dark" ? dark : tone === "auto" ? auto : light;

        const invalid = error ? "ring-rose-500 focus:ring-rose-500" : "";
        const disabledCls = disabled
            ? "opacity-60 cursor-not-allowed bg-gray-100 dark:bg-neutral-800"
            : "";

        const contentCls = `
      z-[60] overflow-hidden rounded-xl border
      border-gray-200 dark:border-neutral-800
      bg-white/95 dark:bg-neutral-900/95
      shadow-xl ring-1 ring-black/5 dark:ring-white/10
      data-[state=open]:animate-in data-[state=open]:fade-in-0 data-[state=open]:zoom-in-95
      data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=closed]:zoom-out-95
      data-[side=bottom]:slide-in-from-top-2
      data-[side=top]:slide-in-from-bottom-2
    `;

        const itemCls = `
      relative select-none rounded-lg px-3 py-2 text-sm
      text-gray-900 dark:text-neutral-100
      outline-none cursor-pointer
      hover:bg-gray-100/80 dark:hover:bg-neutral-800
      focus:bg-gray-100/80 dark:focus:bg-neutral-800
      data-[disabled]:opacity-50 data-[disabled]:cursor-not-allowed
    `;

        const handleValueChange = (v: string) => {
            onChange({ target: { name, value: v } });
        };

        const controlledValue =
            value === "" || value === undefined || value === null
                ? undefined
                : String(value);

        const describedIds =
            [error ? `${selectId}-error` : null, hint ? `${selectId}-hint` : null]
                .filter(Boolean)
                .join(" ") || undefined;

        return (
            <div className={`relative ${containerClassName}`}>
                <label
                    htmlFor={selectId}
                    className="block text-sm font-medium mb-1 text-gray-800 dark:text-gray-200"
                >
                    {label}{" "}
                    {required && (
                        <span className="text-rose-600 dark:text-rose-400">*</span>
                    )}
                </label>

                <Select.Root
                    value={controlledValue}
                    onValueChange={handleValueChange}
                    disabled={disabled}
                >
                    <Select.Trigger
                        id={selectId}
                        ref={ref}
                        aria-describedby={describedIds}
                        aria-invalid={!!error || undefined}
                        className={`${baseTrigger} ${palette} ${invalid} ${disabledCls} ${className} group`}
                    >
                        <Select.Value placeholder={placeholder} />
                        <Select.Icon
                            className="
                ml-2 text-gray-500 dark:text-neutral-400 transition-transform
                group-data-[state=open]:rotate-180
              "
                        >
                            <ChevronDown className="w-4 h-4" />
                        </Select.Icon>
                    </Select.Trigger>

                    <Select.Portal>
                        <Select.Content className={contentCls} position="popper" sideOffset={8}>
                            <Select.Viewport className="p-1 max-h-60">
                                {/* No placeholder item here (Radix disallows empty item values) */}
                                {options.map((opt) => {
                                    const optVal = String(opt.value);
                                    return (
                                        <Select.Item
                                            key={optVal}
                                            value={optVal}
                                            disabled={opt.disabled}
                                            className={itemCls}
                                        >
                      <span className="absolute left-2 flex h-5 w-5 items-center justify-center">
                        <Select.ItemIndicator>
                          <Check className="w-4 h-4" />
                        </Select.ItemIndicator>
                      </span>
                                            <Select.ItemText>
                                                <span className="pl-6">{opt.label}</span>
                                            </Select.ItemText>
                                        </Select.Item>
                                    );
                                })}
                            </Select.Viewport>
                        </Select.Content>
                    </Select.Portal>
                </Select.Root>

                {error ? (
                    <p
                        id={`${selectId}-error`}
                        className="mt-1 text-sm text-rose-600 dark:text-rose-400"
                    >
                        {error}
                    </p>
                ) : hint ? (
                    <p
                        id={`${selectId}-hint`}
                        className="mt-1 text-xs text-gray-600 dark:text-neutral-400"
                    >
                        {hint}
                    </p>
                ) : null}
            </div>
        );
    }
);

AnimatedSelectField.displayName = "AnimatedSelectField";
export default AnimatedSelectField;
