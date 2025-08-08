interface StatCardProps {
    title: string;
    value: string;
    color?: string;
}

const StatCard = ({ title, value, color = 'text-indigo-700' }: StatCardProps) => {
    return (
        <div className="bg-gradient-to-br from-gray-200 to-gray-400 shadow-md rounded-xl p-5">
            <h3 className={`text-xl font-semibold mb-2 ${color}`}>{title}</h3>
            <p className="text-3xl font-bold text-slate-800">{value}</p>
        </div>
    );
};

export default StatCard;