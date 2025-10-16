import React from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import { store } from './store'
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import App from './App.jsx'
import Login from './components/Login'
import Register from './components/Register'
import './index.css'

const ProtectedRoute = ({ children }) => {
    const { isAuthenticated } = useSelector((state) => state.auth)
    return isAuthenticated ? children : <Navigate to="/login" />
}

const PublicRoute = ({ children }) => {
    const { isAuthenticated } = useSelector((state) => state.auth)
    return !isAuthenticated ? children : <Navigate to="/" />
}

const Root = () => {
    return (
        <Router>
            <Routes>
                <Route path="/login" element={
                    <PublicRoute>
                        <Login />
                    </PublicRoute>
                } />
                <Route path="/register" element={
                    <PublicRoute>
                        <Register />
                    </PublicRoute>
                } />
                <Route path="/" element={
                    <ProtectedRoute>
                        <App />
                    </ProtectedRoute>
                } />
                {/* Редирект на логин для любых других путей */}
                <Route path="*" element={<Navigate to="/login" />} />
            </Routes>
        </Router>
    )
}

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        <Provider store={store}>
            <Root />
        </Provider>
    </React.StrictMode>,
)