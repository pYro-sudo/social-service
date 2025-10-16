import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import imageReducer from './slices/imageSlice';

export const store = configureStore({
    reducer: {
        auth: authReducer,
        images: imageReducer,
    },
});

export default store;