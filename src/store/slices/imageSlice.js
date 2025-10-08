import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { imageAPI } from '../../services/api';

// Async thunks
export const fetchAllImages = createAsyncThunk(
    'images/fetchAll',
    async ({ page = 0, size = 12 } = {}, { rejectWithValue }) => {
        try {
            const response = await imageAPI.getAllImages(page, size);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data || 'Failed to fetch images');
        }
    }
);

export const fetchUserImages = createAsyncThunk(
    'images/fetchUser',
    async ({ page = 0, size = 12 } = {}, { rejectWithValue }) => {
        try {
            const response = await imageAPI.getUserImages(page, size);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data || 'Failed to fetch user images');
        }
    }
);

export const uploadImage = createAsyncThunk(
    'images/upload',
    async (formData, { rejectWithValue }) => {
        try {
            const response = await imageAPI.uploadImage(formData);
            return response.data;
        } catch (error) {
            return rejectWithValue(error.response?.data || 'Upload failed');
        }
    }
);

export const deleteImage = createAsyncThunk(
    'images/delete',
    async (imageId, { rejectWithValue }) => {
        try {
            await imageAPI.deleteImage(imageId);
            return imageId;
        } catch (error) {
            return rejectWithValue(error.response?.data || 'Delete failed');
        }
    }
);

const imageSlice = createSlice({
    name: 'images',
    initialState: {
        allImages: {
            content: [],
            totalPages: 0,
            totalElements: 0,
            currentPage: 0,
        },
        userImages: {
            content: [],
            totalPages: 0,
            totalElements: 0,
            currentPage: 0,
        },
        isLoading: false,
        error: null,
        uploadProgress: 0,
    },
    reducers: {
        clearError: (state) => {
            state.error = null;
        },
        setUploadProgress: (state, action) => {
            state.uploadProgress = action.payload;
        },
        clearUploadProgress: (state) => {
            state.uploadProgress = 0;
        },
    },
    extraReducers: (builder) => {
        builder
            // Fetch All Images
            .addCase(fetchAllImages.pending, (state) => {
                state.isLoading = true;
                state.error = null;
            })
            .addCase(fetchAllImages.fulfilled, (state, action) => {
                state.isLoading = false;
                state.allImages = action.payload;
            })
            .addCase(fetchAllImages.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload;
            })
            // Fetch User Images
            .addCase(fetchUserImages.pending, (state) => {
                state.isLoading = true;
                state.error = null;
            })
            .addCase(fetchUserImages.fulfilled, (state, action) => {
                state.isLoading = false;
                state.userImages = action.payload;
            })
            .addCase(fetchUserImages.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload;
            })
            // Upload Image
            .addCase(uploadImage.pending, (state) => {
                state.isLoading = true;
                state.error = null;
                state.uploadProgress = 0;
            })
            .addCase(uploadImage.fulfilled, (state, action) => {
                state.isLoading = false;
                state.uploadProgress = 100;
                // Добавляем новое изображение в начало списков
                state.allImages.content.unshift(action.payload);
                state.userImages.content.unshift(action.payload);
            })
            .addCase(uploadImage.rejected, (state, action) => {
                state.isLoading = false;
                state.error = action.payload;
                state.uploadProgress = 0;
            })
            // Delete Image
            .addCase(deleteImage.fulfilled, (state, action) => {
                const imageId = action.payload;
                // Удаляем изображение из обоих списков
                state.allImages.content = state.allImages.content.filter(img => img.id !== imageId);
                state.userImages.content = state.userImages.content.filter(img => img.id !== imageId);
            });
    },
});

export const { clearError, setUploadProgress, clearUploadProgress } = imageSlice.actions;
export default imageSlice.reducer;