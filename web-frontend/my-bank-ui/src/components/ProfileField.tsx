import React from "react";

interface ProfileFieldProps {
    label: string;
    value?: string | null;
}

const ProfileField: React.FC<ProfileFieldProps> = ({ label, value }) => {
    return (
        <div className="mb-4">
            <p className="text-sm text-gray-500">{label}</p>
            <p className="text-lg font-medium text-gray-800">
                {value?.trim() ? value : <span className="text-gray-400 italic">Not provided</span>}
            </p>
        </div>
    );
};

export default ProfileField;
