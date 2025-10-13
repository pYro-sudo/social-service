import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
});

// const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://social-network.local/api';

// export const api = axios.create({
//   baseURL: API_BASE_URL,
//   headers: {
//     'Content-Type': 'application/json',
//   },
// });

export const imageAPI = {
    getAllImages: (page = 0, size = 12) => api.get(`/api/images?page=${page}&size=${size}`),

    getUserImages: (page = 0, size = 12) => api.get(`/api/images/my?page=${page}&size=${size}`),

    uploadImage: (formData) => api.post('/api/images/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),

    deleteImage: (imageId) => api.delete(`/api/images/${imageId}`),
};

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export const authAPI = {
    login: (credentials) => api.post('/auth/generate-token', credentials),
    register: (userData) => api.post('/auth/add-new-user', userData),
    validateToken: () => api.post('/auth/validate-token'),
    logout: () => api.post('/auth/logout'),
};

export const userAPI = {
    getProfile: () => api.get('/api/users/profile'),
};


export default api;
