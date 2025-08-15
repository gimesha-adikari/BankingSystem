import { useEffect, useMemo, useState } from "react";
import axios from "axios";
import api from "../../api/axios";
import { safeISOString, parseDateFlexible } from "@/utils/dates";
import { copyTextToClipboard } from "@/utils/clipboard";
import { cn } from "@/utils/cn";
import { useAlert } from "@/contexts/use-alert";

type TxType = "DEPOSIT" | "WITHDRAWAL" | "TRANSFER_IN" | "TRANSFER_OUT";

interface Transaction {
    transactionId: string;
    accountId: string;
    type: TxType;
    amount: number;
    balanceAfter: number;
    description?: string | null;
    createdAt: unknown;
}

interface Props {
    accountId: string;
}

const FILTERS = ["ALL", "DEPOSIT", "WITHDRAWAL", "TRANSFER_IN", "TRANSFER_OUT"] as const;
type FilterType = (typeof FILTERS)[number];

const currency = new Intl.NumberFormat(undefined, { style: "currency", currency: "USD" });

const TYPE_STYLE: Record<TxType, { label: string; badge: string; amount: string }> = {
    DEPOSIT: { label: "Deposit", badge: "bg-emerald-500/15 text-emerald-300 ring-1 ring-emerald-500/30", amount: "text-emerald-300" },
    WITHDRAWAL: { label: "Withdrawal", badge: "bg-rose-500/15 text-rose-300 ring-1 ring-rose-500/30", amount: "text-rose-300" },
    TRANSFER_IN: { label: "Transfer In", badge: "bg-sky-500/15 text-sky-300 ring-1 ring-sky-500/30", amount: "text-sky-300" },
    TRANSFER_OUT: { label: "Transfer Out", badge: "bg-amber-500/15 text-amber-300 ring-1 ring-amber-500/30", amount: "text-amber-300" },
};

function toLocalDisplay(input: unknown): string {
    const d = parseDateFlexible(input);
    return d
        ? new Intl.DateTimeFormat(undefined, {
            year: "numeric",
            month: "short",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        }).format(d)
        : "—";
}

function isRecord(v: unknown): v is Record<string, unknown> {
    return !!v && typeof v === "object" && !Array.isArray(v);
}

async function fetchTransactionsWithFallback(accountId: string, signal: AbortSignal) {
    try {
        const res = await api.get(`/api/v1/accounts/${accountId}/transactions`, { signal });
        const d = res.data as unknown;
        if (Array.isArray(d)) return d as Transaction[];
        if (isRecord(d) && Array.isArray(d.data)) return d.data as Transaction[];
        throw new Error("Unexpected response shape");
    } catch (err: unknown) {
        if (axios.isAxiosError(err) && err.response?.status === 200) {
            const d = err.response.data as unknown;
            if (Array.isArray(d)) return d as Transaction[];
            if (isRecord(d) && Array.isArray(d.data)) return d.data as Transaction[];
        }
        const base = import.meta.env.VITE_API_BASE_URL || "";
        const token = localStorage.getItem("token") || "";
        const resp = await fetch(`${base}/api/v1/accounts/${accountId}/transactions`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                ...(token ? { Authorization: `Bearer ${token}` } : {}),
            },
            signal,
        });
        if (resp.ok) {
            const d = (await resp.json()) as unknown;
            if (Array.isArray(d)) return d as Transaction[];
            if (isRecord(d) && Array.isArray(d.data)) return d.data as Transaction[];
        }
        throw err;
    }
}

export function AccountTransactionsList({ accountId }: Props) {
    const [txs, setTxs] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [q, setQ] = useState("");
    const [typeFilter, setTypeFilter] = useState<FilterType>("ALL");
    const { showAlert } = useAlert();

    useEffect(() => {
        const ac = new AbortController();
        (async () => {
            try {
                setLoading(true);
                setError(null);
                const rows = await fetchTransactionsWithFallback(accountId, ac.signal);
                setTxs(rows);
            } catch (e: unknown) {
                if ((e as { name?: string })?.name === "AbortError") return;
                let msg = "Failed to load transactions";
                if (axios.isAxiosError(e)) msg = e.response?.data?.message || e.message || msg;
                else if (e instanceof Error) msg = e.message;
                setError(msg);
            } finally {
                setLoading(false);
            }
        })();
        return () => ac.abort();
    }, [accountId]);

    const filtered = useMemo(() => {
        const needle = q.trim().toLowerCase();
        return txs.filter((t) => {
            const matchesType = typeFilter === "ALL" || t.type === typeFilter;
            const hay = `${TYPE_STYLE[t.type].label} ${t.description ?? ""}`.toLowerCase();
            const matchesText = needle.length === 0 || hay.includes(needle);
            return matchesType && matchesText;
        });
    }, [txs, q, typeFilter]);

    function downloadCsv(rows: Transaction[]) {
        const headers = ["Transaction ID", "Date/Time", "Type", "Amount", "Balance After", "Description"];
        const body = rows.map((r) => [
            r.transactionId,
            safeISOString(r.createdAt),
            TYPE_STYLE[r.type].label,
            r.amount,
            r.balanceAfter,
            (r.description ?? "").replaceAll('"', '""'),
        ]);
        const csv = [headers, ...body]
            .map((cols) => cols.map((v) => `"${String(v)}"`).join(","))
            .join("\n");
        const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "transactions.csv";
        a.click();
        URL.revokeObjectURL(url);
    }

    return (
        <div
            className={cn(
                "rounded-2xl shadow-2xl ring-1 ring-white/10",
                "bg-neutral-900/75 backdrop-blur",
                "motion-safe:animate-[panelIn_.22s_ease-out]"
            )}
        >
            <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between p-4 sm:p-6">
                <div>
                    <h2 className="text-xl sm:text-2xl font-semibold text-white tracking-tight">Transactions</h2>
                    <p className="text-sm text-neutral-300">Account activity (includes opening deposit).</p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={() => downloadCsv(filtered)}
                        className="px-3 py-2 rounded-xl bg-indigo-600 text-white hover:bg-indigo-700 active:scale-[.98]
                       focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition shadow-sm"
                    >
                        Export CSV
                    </button>
                    <button
                        onClick={async () => {
                            const ac = new AbortController();
                            try {
                                setLoading(true);
                                setError(null);
                                const rows = await fetchTransactionsWithFallback(accountId, ac.signal);
                                setTxs(rows);
                            } catch (e: unknown) {
                                if ((e as { name?: string })?.name === "AbortError") return;
                                let msg = "Failed to load transactions";
                                if (axios.isAxiosError(e)) msg = e.response?.data?.message || e.message || msg;
                                else if (e instanceof Error) msg = e.message;
                                setError(msg);
                            } finally {
                                setLoading(false);
                            }
                        }}
                        className="px-3 py-2 rounded-xl bg-white/5 text-indigo-200 ring-1 ring-white/10 hover:bg-white/10
                       hover:ring-white/20 transition focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500"
                    >
                        Refresh
                    </button>
                </div>
            </div>

            <div className="px-4 sm:px-6 pb-4 flex flex-col gap-3 sm:flex-row sm:items-center">
                <div className="flex-1">
                    <div className="relative">
            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M21 21l-4.35-4.35" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
              </svg>
            </span>
                        <input
                            value={q}
                            onChange={(e) => setQ(e.target.value)}
                            placeholder="Search type or description…"
                            className="w-full rounded-xl bg-neutral-900/80 text-neutral-100 placeholder:text-neutral-400
                         pl-10 pr-3 py-2 ring-1 ring-neutral-700 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>
                </div>
                <div className="flex gap-2 flex-wrap">
                    {FILTERS.map((t) => (
                        <button
                            key={t}
                            onClick={() => setTypeFilter(t)}
                            className={cn(
                                "px-3 py-1.5 rounded-full text-sm ring-1 transition",
                                "focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500",
                                t === typeFilter
                                    ? "bg-indigo-600 text-white ring-indigo-500 shadow-sm"
                                    : "bg-white/5 text-indigo-200 ring-white/10 hover:bg-white/10 hover:ring-white/20"
                            )}
                            title={t === "ALL" ? "All types" : TYPE_STYLE[t as Exclude<FilterType, "ALL">].label}
                        >
                            {t === "ALL" ? "All" : TYPE_STYLE[t as Exclude<FilterType, "ALL">].label}
                        </button>
                    ))}
                </div>
            </div>

            <div className="px-4 sm:px-6 pb-6">
                <div className="rounded-2xl overflow-hidden ring-1 ring-white/10">
                    <div className="max-h-[60vh] overflow-auto">
                        <table className="min-w-full text-sm">
                            <thead className="sticky top-0 z-10 bg-neutral-950/80 backdrop-blur">
                            <tr className="text-left text-indigo-200/90">
                                <th className="px-4 py-3 border-b border-white/10 font-medium">Date/Time</th>
                                <th className="px-4 py-3 border-b border-white/10 font-medium">Type</th>
                                <th className="px-4 py-3 border-b border-white/10 font-medium text-right">Amount</th>
                                <th className="px-4 py-3 border-b border-white/10 font-medium text-right">Balance After</th>
                                <th className="px-4 py-3 border-b border-white/10 font-medium">Description</th>
                                <th className="px-4 py-3 border-b border-white/10 font-medium">ID</th>
                            </tr>
                            </thead>

                            {loading && (
                                <tbody>
                                {Array.from({ length: 6 }).map((_, i) => (
                                    <tr key={i} className={i % 2 ? "bg-neutral-900/50" : ""}>
                                        {Array.from({ length: 6 }).map((__, j) => (
                                            <td key={j} className="px-4 py-3 border-b border-white/10">
                                                <div className="animate-pulse h-4 w-full max-w-[180px] bg-neutral-700/60 rounded" />
                                            </td>
                                        ))}
                                    </tr>
                                ))}
                                </tbody>
                            )}

                            {!loading && error && (
                                <tbody>
                                <tr>
                                    <td colSpan={6} className="px-4 py-6 text-rose-300 bg-rose-500/5">
                                        {error}
                                    </td>
                                </tr>
                                </tbody>
                            )}

                            {!loading && !error && filtered.length === 0 && (
                                <tbody>
                                <tr>
                                    <td colSpan={6} className="px-6 py-10 text-center">
                                        <div className="inline-flex flex-col items-center gap-2">
                                            <div className="text-3xl">🪙</div>
                                            <div className="text-indigo-100 font-medium">No transactions</div>
                                            <div className="text-neutral-400 text-sm">Try changing the filter or create a new transaction.</div>
                                        </div>
                                    </td>
                                </tr>
                                </tbody>
                            )}

                            {!loading && !error && filtered.length > 0 && (
                                <tbody>
                                {filtered.map((tx, idx) => {
                                    const tStyle = TYPE_STYLE[tx.type];
                                    return (
                                        <tr
                                            key={tx.transactionId}
                                            className={cn(
                                                idx % 2 === 1 && "bg-neutral-900/50",
                                                "hover:bg-neutral-800/70 transition-colors"
                                            )}
                                        >
                                            <td className="px-4 py-3 border-b border-white/10 text-indigo-100 whitespace-nowrap">
                                                {toLocalDisplay(tx.createdAt)}
                                            </td>
                                            <td className="px-4 py-3 border-b border-white/10">
                          <span className={cn("px-2.5 py-1 rounded-full text-xs font-medium", tStyle.badge)}>
                            {tStyle.label}
                          </span>
                                            </td>
                                            <td className={cn("px-4 py-3 border-b border-white/10 text-right font-medium", tStyle.amount)}>
                                                {currency.format(tx.amount)}
                                            </td>
                                            <td className="px-4 py-3 border-b border-white/10 text-right text-indigo-200 tabular-nums">
                                                {currency.format(tx.balanceAfter)}
                                            </td>
                                            <td className="px-4 py-3 border-b border-white/10 text-indigo-200">
                                                {tx.description || <span className="text-neutral-500">—</span>}
                                            </td>
                                            <td className="px-4 py-3 border-b border-white/10">
                                                <div className="flex items-center gap-2">
                                                    <code className="text-xs text-neutral-400">{tx.transactionId.slice(0, 8)}…</code>
                                                    <button
                                                        onClick={async () => {
                                                            const result = await copyTextToClipboard(tx.transactionId);
                                                            if (result === "copied") showAlert("ID copied", "success", 1500);
                                                        }}
                                                        className="text-xs px-2 py-1 rounded bg-white/5 text-indigo-200 ring-1 ring-white/10 hover:bg-white/10
                                         focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 transition"
                                                        title="Copy ID"
                                                    >
                                                        Copy
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    );
                                })}
                                </tbody>
                            )}
                        </table>
                    </div>
                </div>
            </div>

            <style>
                {`
          @keyframes panelIn {
            from { opacity: 0; transform: translateY(6px) scale(0.98); }
            to   { opacity: 1; transform: translateY(0) scale(1); }
          }
        `}
            </style>
        </div>
    );
}
