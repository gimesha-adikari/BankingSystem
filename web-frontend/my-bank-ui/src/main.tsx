import React from "react";
import {createRoot} from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import AuthProvider from "@/contexts/AuthProvider";
import AlertProvider from "@/contexts/AlertProvider.tsx";
import "@/index.css";

createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
        <AuthProvider>
            <AlertProvider>
                <BrowserRouter>
                    <App />
                </BrowserRouter>
            </AlertProvider>
        </AuthProvider>
    </React.StrictMode>
);
