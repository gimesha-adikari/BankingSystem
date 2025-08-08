import React from "react";

const Footer: React.FC = () => {
    return (
        <footer className="bg-gray-900 text-white py-6 px-4 md:px-10 text-center shadow-inner">
            <div className="space-y-2 text-sm">
                <p className="font-semibold text-indigo-300">
                    &copy; {new Date().getFullYear()} MyBank. All rights reserved.
                </p>
                <p className="opacity-80">
                    Contact us:{" "}
                    <a
                        href="mailto:support@mybank.com"
                        className="underline hover:text-indigo-300"
                    >
                        support@mybank.com
                    </a>{" "}
                    | +1 800 123 4567
                </p>
            </div>
        </footer>
    );
};

export default Footer;
