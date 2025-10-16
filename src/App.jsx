import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { logoutUser } from './store/slices/authSlice';
import { fetchAllImages, fetchUserImages, deleteImage } from './store/slices/imageSlice';
import ImageGallery from './components/ImageGallery';
import Pagination from './components/Pagination';
import ImageUpload from './components/ImageUpload';
import ImageModal from './components/ImageModal';
import { useNavigate } from 'react-router-dom';

const Dashboard = () => {
    const dispatch = useDispatch();
    const navigate = useNavigate();
    const { user, isAuthenticated } = useSelector((state) => state.auth);
    const { allImages, userImages, isLoading } = useSelector((state) => state.images);

    const [activeTab, setActiveTab] = useState('all');
    const [allImagesPage, setAllImagesPage] = useState(0);
    const [userImagesPage, setUserImagesPage] = useState(0);
    const [selectedImage, setSelectedImage] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const pageSize = 12;

    // Редирект если не авторизован
    useEffect(() => {
        if (!isAuthenticated) {
            navigate('/login');
        }
    }, [isAuthenticated, navigate]);

    useEffect(() => {
        if (isAuthenticated) {
            dispatch(fetchAllImages({ page: allImagesPage, size: pageSize }));
        }
    }, [dispatch, allImagesPage, isAuthenticated]);

    useEffect(() => {
        if (isAuthenticated && user?.id) {
            dispatch(fetchUserImages({ userId: user.id, page: userImagesPage, size: pageSize }));
        }
    }, [dispatch, userImagesPage, user, isAuthenticated]);

    const handleLogout = () => {
        dispatch(logoutUser());
    };

    const handleImageClick = (image) => {
        setSelectedImage(image);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setSelectedImage(null);
    };

    const handleDeleteImage = (imageId) => {
        if (window.confirm('Are you sure you want to delete this image?')) {
            dispatch(deleteImage(imageId)).then(() => {
                if (selectedImage?.id === imageId) {
                    handleCloseModal();
                }
            });
        }
    };

    const handleUploadSuccess = () => {
        if (activeTab === 'all') {
            dispatch(fetchAllImages({ page: allImagesPage, size: pageSize }));
        } else if (user?.id) {
            dispatch(fetchUserImages({ userId: user.id, page: userImagesPage, size: pageSize }));
        }
    };

    const currentImages = activeTab === 'all' ? allImages : userImages;
    const currentPage = activeTab === 'all' ? allImagesPage : userImagesPage;
    const setCurrentPage = activeTab === 'all' ? setAllImagesPage : setUserImagesPage;

    // Если не авторизован, не показываем ничего
    if (!isAuthenticated) {
        return null;
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-gray-50 to-gray-100">
            {/* Header */}
            <header className="bg-white shadow-sm border-b border-gray-200">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center py-6">
                        <div className="flex items-center space-x-3">
                            <div className="w-10 h-10 bg-gradient-to-r from-indigo-500 to-purple-600 rounded-lg flex items-center justify-center">
                                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                </svg>
                            </div>
                            <div>
                                <h1 className="text-2xl font-bold text-gray-900">Photo Gallery</h1>
                                <p className="text-sm text-gray-500">Share and discover amazing photos</p>
                            </div>
                        </div>

                        <div className="flex items-center space-x-4">
                            <div className="text-right">
                                <p className="text-sm font-medium text-gray-900">Welcome back</p>
                                <p className="text-sm text-gray-500">{user?.username}</p>
                            </div>
                            <div className="w-10 h-10 bg-gradient-to-r from-indigo-400 to-indigo-600 rounded-full flex items-center justify-center text-white font-semibold">
                                {user?.username?.charAt(0).toUpperCase()}
                            </div>
                            <button
                                onClick={handleLogout}
                                className="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition-colors font-medium"
                            >
                                Logout
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {/* Stats and Actions Card */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6 mb-8">
                    <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between">
                        <div className="flex-1">
                            <h2 className="text-lg font-semibold text-gray-900 mb-2">Your Gallery Dashboard</h2>
                            <p className="text-gray-600">Manage your photos and explore community content</p>
                        </div>
                        <div className="flex items-center space-x-4 mt-4 lg:mt-0">
                            <div className="bg-indigo-50 rounded-lg px-4 py-2">
                                <p className="text-sm text-indigo-600 font-medium">Total Images</p>
                                <p className="text-2xl font-bold text-indigo-700">{allImages.totalElements || 0}</p>
                            </div>
                            <ImageUpload onUploadSuccess={handleUploadSuccess} />
                        </div>
                    </div>
                </div>

                {/* Navigation Tabs */}
                <div className="bg-white rounded-xl shadow-sm border border-gray-200 mb-8">
                    <div className="border-b border-gray-200">
                        <nav className="flex space-x-8 px-6">
                            <button
                                onClick={() => setActiveTab('all')}
                                className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                                    activeTab === 'all'
                                        ? 'border-indigo-500 text-indigo-600'
                                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                }`}
                            >
                                <div className="flex items-center space-x-2">
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                                    </svg>
                                    <span>All Images</span>
                                </div>
                            </button>
                            <button
                                onClick={() => setActiveTab('my')}
                                className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                                    activeTab === 'my'
                                        ? 'border-indigo-500 text-indigo-600'
                                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                }`}
                            >
                                <div className="flex items-center space-x-2">
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                    </svg>
                                    <span>My Images</span>
                                </div>
                            </button>
                        </nav>
                    </div>

                    {/* Tab Content */}
                    <div className="p-6">
                        {/* Loading State */}
                        {isLoading && (
                            <div className="text-center py-12">
                                <div className="inline-flex items-center space-x-3 text-gray-600">
                                    <svg className="animate-spin h-5 w-5 text-indigo-600" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                    <span>Loading images...</span>
                                </div>
                            </div>
                        )}

                        {/* Content */}
                        {!isLoading && (
                            <>
                                <div className="mb-6">
                                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                                        {activeTab === 'all' ? 'Community Gallery' : 'Your Uploaded Images'}
                                    </h3>
                                    <p className="text-gray-600">
                                        {activeTab === 'all'
                                            ? 'Discover amazing photos from our community'
                                            : 'Manage and view your personal photo collection'
                                        }
                                    </p>
                                </div>

                                <ImageGallery
                                    images={currentImages.content || []}
                                    onImageClick={handleImageClick}
                                    onDeleteImage={handleDeleteImage}
                                    showDeleteButton={activeTab === 'my'}
                                    currentUser={user}
                                />

                                {/* Pagination */}
                                {currentImages.totalPages > 1 && (
                                    <div className="mt-8 border-t border-gray-200 pt-6">
                                        <Pagination
                                            currentPage={currentPage}
                                            totalPages={currentImages.totalPages}
                                            onPageChange={setCurrentPage}
                                        />
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </main>

            {/* Image Modal */}
            <ImageModal
                image={selectedImage}
                isOpen={isModalOpen}
                onClose={handleCloseModal}
                currentUser={user}
            />
        </div>
    );
};

export default Dashboard;