import React from 'react';

const ImageGallery = ({
                          images,
                          onImageClick,
                          onDeleteImage,
                          showDeleteButton = false,
                          currentUser
                      }) => {
    if (!images || images.length === 0) {
        return (
            <div className="text-center py-16">
                <div className="max-w-md mx-auto">
                    <svg className="w-24 h-24 text-gray-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    <h3 className="text-lg font-medium text-gray-900 mb-2">No images found</h3>
                    <p className="text-gray-500">
                        {showDeleteButton
                            ? "You haven't uploaded any images yet. Start by uploading your first photo!"
                            : "No images available in the gallery yet."
                        }
                    </p>
                </div>
            </div>
        );
    }

    return (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {images.map((image) => (
                <div
                    key={image.id}
                    className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden hover:shadow-md transition-shadow duration-300 group"
                >
                    {/* Image Container */}
                    <div
                        className="relative aspect-w-1 aspect-h-1 bg-gray-100 cursor-pointer overflow-hidden"
                        onClick={() => onImageClick && onImageClick(image)}
                    >
                        <img
                            src={image.url || '/placeholder-image.jpg'}
                            alt={image.title || image.description || 'Image'}
                            className="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-300"
                        />

                        {/* Overlay on hover */}
                        <div className="absolute inset-0 bg-black bg-opacity-0 group-hover:bg-opacity-20 transition-all duration-300 flex items-center justify-center">
                            <div className="opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                                <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                </svg>
                            </div>
                        </div>
                    </div>

                    {/* Image Info */}
                    <div className="p-4">
                        <h3 className="font-medium text-gray-900 mb-2 line-clamp-2">
                            {image.title || image.description || 'Untitled'}
                        </h3>

                        <div className="flex items-center justify-between text-sm text-gray-600 mb-3">
                            <div className="flex items-center space-x-2">
                                <div className="w-6 h-6 bg-gradient-to-r from-blue-500 to-blue-600 rounded-full flex items-center justify-center text-white text-xs font-medium">
                                    {image.owner?.username?.charAt(0).toUpperCase() || 'U'}
                                </div>
                                <span className="font-medium">{image.owner?.username || 'Unknown'}</span>
                            </div>
                            <span className="text-gray-500">
                                {image.createdAt ? new Date(image.createdAt).toLocaleDateString() : 'Unknown date'}
                            </span>
                        </div>

                        {/* Action Buttons */}
                        {showDeleteButton && currentUser && image.owner?.id === currentUser.id && (
                            <button
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onDeleteImage && onDeleteImage(image.id);
                                }}
                                className="w-full bg-red-50 text-red-600 py-2 px-3 rounded-md hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-1 transition-colors text-sm font-medium flex items-center justify-center space-x-2"
                            >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                                <span>Delete Image</span>
                            </button>
                        )}
                    </div>
                </div>
            ))}
        </div>
    );
};

export default ImageGallery;