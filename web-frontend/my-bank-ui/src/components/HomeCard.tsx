import React from 'react';

interface HomeCardProps {
    title: string;
    description: string;
    link: string;
    buttonText: string;
}

const HomeCard: React.FC<HomeCardProps> = ({ title, description, link, buttonText }) => {
    return (
        <div className="bg-gradient-to-br from-gray-600 to-gray-800 border border-gray-900 shadow-md rounded-lg p-6 flex flex-col justify-between">
            <h3 className="text-xl font-semibold text-indigo-300 mb-3">{title}</h3>
            <p className="text-indigo-400 mb-5">{description}</p>
            <a
                href={link}
                className="bg-indigo-600 hover:bg-indigo-700 transition text-white px-4 py-2 rounded"
            >
                {buttonText}
            </a>
        </div>
    );
};

export default HomeCard;
