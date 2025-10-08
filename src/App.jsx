import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from './store/slices/authSlice';
import { fetchAllImages, fetchUserImages, deleteImage } from './store/slices/imageSlice';
import ImageGallery from './components/ImageGallery';
import Pagination from './components/Pagination';
import ImageUpload from './components/ImageUpload';

const Dashboard = () => {
    const dispatch = useDispatch();
    const { user } = useSelector((state) => state.auth);
    const { allImages, userImages, isLoading } = useSelector((state) => state.images);

    const [activeTab, setActiveTab] = useState('all');
    const [allImagesPage, setAllImagesPage] = useState(0);
    const [userImagesPage, setUserImagesPage] = useState(0);
    const pageSize = 12;

    useEffect(() => {
        dispatch(fetchAllImages({ page: allImagesPage, size: pageSize }));
    }, [dispatch, allImagesPage]);

    useEffect(() => {
        dispatch(fetchUserImages({ page: userImagesPage, size: pageSize }));
    }, [dispatch, userImagesPage]);

    const handleLogout = () => {
        dispatch(logout());
    };

    const handleImageClick = (image) => {
        // Открыть изображение в полном размере или показать детали
        console.log('Image clicked:', image);
        // Можно реализовать модальное окно с деталями изображения
    };

    const handleDeleteImage = (imageId) => {
        if (window.confirm('Are you sure you want to delete this image?')) {
            dispatch(deleteImage(imageId));
        }
    };

    const handleUploadSuccess = () => {
        // Обновляем галереи после успешной загрузки
        if (activeTab === 'all') {
            dispatch(fetchAllImages({ page: allImagesPage, size: pageSize }));
        } else {
            dispatch(fetchUserImages({ page: userImagesPage, size: pageSize }));
        }
    };

    const currentImages = activeTab === 'all' ? allImages : userImages;
    const currentPage = activeTab === 'all' ? allImagesPage : userImagesPage;
    const setCurrentPage = activeTab === 'all' ? setAllImagesPage : setUserImagesPage;

    return (
        <div className="min-h-screen bg-gray-100">
            {/* Header */}
            <header className="bg-white shadow-sm border-b">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center py-4">
                        <h1 className="text-2xl font-bold text-gray-900">Photo Gallery</h1>
                        <div className="flex items-center space-x-4">
                            <span className="text-gray-700">Welcome, {user?.username}!</span>
                            <button
                                onClick={handleLogout}
                                className="bg-red-600 text-white px-4 py-2 rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500"
                            >
                                Logout
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Upload Button */}
                <div className="mb-8 flex justify-between items-center">
                    <div className="flex space-x-4">
                        <button
                            onClick={() => setActiveTab('all')}
                            className={`px-4 py-2 rounded-md font-medium ${
                                activeTab === 'all'
                                    ? 'bg-indigo-600 text-white'
                                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                            }`}
                        >
                            All Images
                        </button>
                        <button
                            onClick={() => setActiveTab('my')}
                            className={`px-4 py-2 rounded-md font-medium ${
                                activeTab === 'my'
                                    ? 'bg-indigo-600 text-white'
                                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                            }`}
                        >
                            My Images
                        </button>
                    </div>
                    <ImageUpload onUploadSuccess={handleUploadSuccess} />
                </div>

                {/* Loading State */}
                {isLoading && (
                    <div className="text-center py-12">
                        <div className="text-lg">Loading images...</div>
                    </div>
                )}

                {/* Image Gallery */}
                {!isLoading && (
                    <>
                        <ImageGallery
                            images={currentImages.content}
                            title={activeTab === 'all' ? 'All Images' : 'My Images'}
                            onImageClick={handleImageClick}
                            onDeleteImage={handleDeleteImage}
                            showDeleteButton={activeTab === 'my'}
                            currentUser={user}
                        />

                        {/* Pagination */}
                        {currentImages.totalPages > 1 && (
                            <Pagination
                                currentPage={currentPage}
                                totalPages={currentImages.totalPages}
                                onPageChange={setCurrentPage}
                                className="mt-8"
                            />
                        )}
                    </>
                )}
            </main>
        </div>
    );
};

export default Dashboard;