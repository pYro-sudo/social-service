import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080'; // API Gateway

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
});

export const authAPI = {
    login: (credentials) => api.post('/auth/generate-token', credentials),
    register: (userData) => api.post('/auth/add-new-user', userData),
    validateToken: () => api.get('/auth/validate-token'),
    logout: () => api.post('/auth/logout'),
    getUserInfo: () => api.get('/auth/user-info'),
};

export const imageAPI = {
    getAllImages: (page = 0, size = 12) =>
        api.get(`/api/images?page=${page}&size=${size}`),

    getUserImages: (userId, page = 0, size = 12) =>
        api.get(`/api/images/user/${userId}?page=${page}&size=${size}`),

    uploadImage: (formData) => api.post('/api/images/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    }),

    deleteImage: (imageId) => api.delete(`/api/images/${imageId}`),

    getImageById: (imageId) => api.get(`/api/images/${imageId}`),
};

// Comments API
export const commentAPI = {
    getImageComments: (imageId) => api.get(`/api/comments/image/${imageId}`),
    createComment: (commentData) => api.post('/api/comments', commentData),
    deleteComment: (commentId) => api.delete(`/api/comments/${commentId}`),
    getCommentById: (commentId) => api.get(`/api/comments/${commentId}`),
};

// Likes API
export const likeAPI = {
    toggleLike: (imageId, userId) => api.post(`/api/likes/images/${imageId}/likes`, null, {
        headers: {
            'X-User-Id': userId
        }
    }),
    getImageLikes: (imageId) => api.get(`/api/likes/image/${imageId}/count`),
    checkLike: (userId, imageId) => api.get(`/api/likes/check?userId=${userId}&imageId=${imageId}`),
    getImageLikesList: (imageId) => api.get(`/api/likes/image/${imageId}`),
};

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
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

export default api;