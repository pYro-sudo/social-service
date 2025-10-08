import React from 'react';

const ImageGallery = ({
                          images,
                          title,
                          onImageClick,
                          onDeleteImage,
                          showDeleteButton = false,
                          currentUser
                      }) => {
    if (!images || images.length === 0) {
        return (
            <div className="text-center py-12">
                <p className="text-gray-500 text-lg">No images found</p>
            </div>
        );
    }

    return (
        <div className="mb-8">
            {title && (
                <h2 className="text-2xl font-bold mb-6 text-gray-800">{title}</h2>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                {images.map((image) => (
                    <div
                        key={image.id}
                        className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300"
                    >
                        <div
                            className="aspect-w-1 aspect-h-1 bg-gray-200 cursor-pointer"
                            onClick={() => onImageClick && onImageClick(image)}
                        >
                            <img
                                src={image.url || '/placeholder-image.jpg'}
                                alt={image.title || 'Image'}
                                className="w-full h-48 object-cover"
                            />
                        </div>

                        <div className="p-4">
                            <h3 className="font-semibold text-gray-800 mb-2 truncate">
                                {image.title || 'Untitled'}
                            </h3>

                            <div className="flex justify-between items-center text-sm text-gray-600">
                                <span>By {image.owner?.username || 'Unknown'}</span>
                                <span>{new Date(image.createdAt).toLocaleDateString()}</span>
                            </div>

                            {showDeleteButton && currentUser && image.owner?.id === currentUser.id && (
                                <button
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onDeleteImage && onDeleteImage(image.id);
                                    }}
                                    className="mt-3 w-full bg-red-600 text-white py-2 px-3 rounded-md hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 text-sm"
                                >
                                    Delete
                                </button>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default ImageGallery;