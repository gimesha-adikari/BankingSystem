import React, { ReactNode } from "react";
import Header from "./Header";
import Footer from "./Footer";

type HomeLayoutProps = {
    children: ReactNode;
};

const HomeLayout: React.FC<HomeLayoutProps> = ({ children }) => {
    return (
        <div className="min-h-screen flex flex-col bg-gray-600 text-gray-800">
            <Header />
            <main className="flex-grow flex flex-col justify-center items-center text-center px-6 py-20">
                {children}
            </main>
            <Footer />
        </div>
    );
};

export default HomeLayout;
