import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { likeAPI, commentAPI } from '../services/api';

const ImageModal = ({ image, isOpen, onClose, currentUser }) => {
    const [comments, setComments] = useState([]);
    const [newComment, setNewComment] = useState('');
    const [isLiked, setIsLiked] = useState(false);
    const [likeCount, setLikeCount] = useState(0);
    const [isLoading, setIsLoading] = useState(false);

    const dispatch = useDispatch();

    useEffect(() => {
        if (isOpen && image) {
            loadImageDetails();
        }
    }, [isOpen, image]);

    const loadImageDetails = async () => {
        if (!image?.id || !currentUser?.id) return;

        try {
            // Загружаем комментарии
            const commentsResponse = await commentAPI.getImageComments(image.id);
            setComments(commentsResponse.data || []);

            // Проверяем лайк
            const likeCheckResponse = await likeAPI.checkLike(currentUser.id, image.id);
            setIsLiked(likeCheckResponse.data || false);

            // Получаем количество лайков
            const likeCountResponse = await likeAPI.getImageLikes(image.id);
            setLikeCount(likeCountResponse.data?.count || 0);

        } catch (error) {
            console.error('Error loading image details:', error);
        }
    };

    const handleLike = async () => {
        if (!currentUser?.id || !image?.id) return;

        try {
            setIsLoading(true);
            await likeAPI.toggleLike(image.id, currentUser.id);

            // Обновляем состояние
            setIsLiked(!isLiked);
            setLikeCount(prev => isLiked ? prev - 1 : prev + 1);
        } catch (error) {
            console.error('Error toggling like:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleAddComment = async (e) => {
        e.preventDefault();
        if (!newComment.trim() || !currentUser?.id || !image?.id) return;

        try {
            setIsLoading(true);
            const commentData = {
                userId: currentUser.id,
                imageId: image.id,
                content: newComment.trim()
            };

            await commentAPI.createComment(commentData);

            // Обновляем комментарии
            setNewComment('');
            await loadImageDetails();

        } catch (error) {
            console.error('Error adding comment:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const handleDeleteComment = async (commentId) => {
        if (!commentId) return;

        try {
            await commentAPI.deleteComment(commentId);
            // Обновляем комментарии
            await loadImageDetails();
        } catch (error) {
            console.error('Error deleting comment:', error);
        }
    };

    if (!isOpen || !image) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-75 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden">
                {/* Header */}
                <div className="flex justify-between items-center p-6 border-b border-gray-200">
                    <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-blue-600 rounded-full flex items-center justify-center text-white font-semibold">
                            {image.owner?.username?.charAt(0).toUpperCase() || 'U'}
                        </div>
                        <div>
                            <h2 className="text-xl font-bold text-gray-900">
                                {image.title || image.description || 'Untitled'}
                            </h2>
                            <p className="text-sm text-gray-500">by {image.owner?.username || 'Unknown'}</p>
                        </div>
                    </div>
                    <button
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-600 p-2 rounded-full hover:bg-gray-100 transition-colors"
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <div className="flex flex-col lg:flex-row max-h-[calc(90vh-80px)]">
                    {/* Image Section */}
                    <div className="lg:w-2/3 bg-gray-100 flex items-center justify-center p-4">
                        <img
                            src={image.url}
                            alt={image.title || image.description || 'Image'}
                            className="max-h-[500px] max-w-full object-contain rounded-lg"
                        />
                    </div>

                    {/* Details Section */}
                    <div className="lg:w-1/3 flex flex-col border-l border-gray-200">
                        {/* Actions */}
                        <div className="p-4 border-b border-gray-200">
                            <div className="flex items-center justify-between mb-4">
                                <button
                                    onClick={handleLike}
                                    disabled={isLoading}
                                    className={`flex items-center space-x-2 p-2 rounded-lg transition-colors ${
                                        isLiked
                                            ? 'text-red-600 bg-red-50'
                                            : 'text-gray-600 hover:text-red-600 hover:bg-gray-50'
                                    }`}
                                >
                                    <svg
                                        className="w-6 h-6"
                                        fill={isLiked ? "currentColor" : "none"}
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={isLiked ? 0 : 2}
                                            d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                                        />
                                    </svg>
                                    <span className="font-medium">{likeCount}</span>
                                </button>

                                <div className="text-sm text-gray-500">
                                    {image.createdAt && new Date(image.createdAt).toLocaleDateString('en-US', {
                                        year: 'numeric',
                                        month: 'long',
                                        day: 'numeric'
                                    })}
                                </div>
                            </div>

                            {image.description && (
                                <p className="text-gray-700 text-sm leading-relaxed">
                                    {image.description}
                                </p>
                            )}
                        </div>

                        {/* Comments Section */}
                        <div className="flex-1 overflow-y-auto">
                            <div className="p-4">
                                <h3 className="font-semibold text-gray-900 mb-3">
                                    Comments ({comments.length})
                                </h3>

                                {/* Comments List */}
                                <div className="space-y-3 mb-4">
                                    {comments.map((comment) => (
                                        <div key={comment.id} className="bg-gray-50 rounded-lg p-3">
                                            <div className="flex justify-between items-start mb-1">
                                                <div className="flex items-center space-x-2">
                                                    <div className="w-6 h-6 bg-gradient-to-r from-green-500 to-green-600 rounded-full flex items-center justify-center text-white text-xs font-medium">
                                                        {comment.user?.username?.charAt(0).toUpperCase() || 'U'}
                                                    </div>
                                                    <span className="text-sm font-medium text-gray-900">
                                                        {comment.user?.username || 'Unknown'}
                                                    </span>
                                                </div>
                                                {currentUser?.id === comment.user?.id && (
                                                    <button
                                                        onClick={() => handleDeleteComment(comment.id)}
                                                        className="text-red-500 hover:text-red-700 text-xs"
                                                    >
                                                        Delete
                                                    </button>
                                                )}
                                            </div>
                                            <p className="text-sm text-gray-700 ml-8">
                                                {comment.content}
                                            </p>
                                            <p className="text-xs text-gray-500 ml-8 mt-1">
                                                {comment.createdAt && new Date(comment.createdAt).toLocaleDateString()}
                                            </p>
                                        </div>
                                    ))}

                                    {comments.length === 0 && (
                                        <p className="text-center text-gray-500 text-sm py-4">
                                            No comments yet. Be the first to comment!
                                        </p>
                                    )}
                                </div>
                            </div>
                        </div>

                        {/* Add Comment Form */}
                        <div className="p-4 border-t border-gray-200">
                            <form onSubmit={handleAddComment} className="space-y-3">
                                <textarea
                                    value={newComment}
                                    onChange={(e) => setNewComment(e.target.value)}
                                    placeholder="Add a comment..."
                                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm resize-none"
                                    rows="2"
                                    disabled={isLoading}
                                />
                                <div className="flex justify-between items-center">
                                    <span className="text-xs text-gray-500">
                                        {newComment.length}/500
                                    </span>
                                    <button
                                        type="submit"
                                        disabled={!newComment.trim() || isLoading}
                                        className="px-4 py-2 bg-indigo-600 text-white text-sm rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                                    >
                                        {isLoading ? 'Posting...' : 'Post Comment'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ImageModal;