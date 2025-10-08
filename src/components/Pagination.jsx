import React from 'react';

const Pagination = ({
    currentPage,
    totalPages,
    onPageChange,
    className = ''
}) => {
    if (totalPages <= 1) return null;

    const pages = [];
    const showPages = 5; // Количество отображаемых страниц

    let startPage = Math.max(0, currentPage - Math.floor(showPages / 2));
    let endPage = Math.min(totalPages - 1, startPage + showPages - 1);

    if (endPage - startPage + 1 < showPages) {
        startPage = Math.max(0, endPage - showPages + 1);
    }

    // Кнопка "Назад"
    if (currentPage > 0) {
        pages.push(
            <button
                key="prev"
                onClick={() => onPageChange(currentPage - 1)}
                className="px-3 py-2 border border-gray-300 rounded-l-md bg-white text-gray-500 hover:bg-gray-50"
            >
                Previous
            </button>
        );
    }

    // Первая страница
    if (startPage > 0) {
        pages.push(
            <button
                key={0}
                onClick={() => onPageChange(0)}
                className="px-3 py-2 border border-gray-300 bg-white text-gray-500 hover:bg-gray-50"
            >
                1
            </button>
        );
        if (startPage > 1) {
            pages.push(
                <span key="start-ellipsis" className="px-3 py-2 border border-gray-300 bg-gray-50">
                    ...
                </span>
            );
        }
    }

    // Страницы
    for (let i = startPage; i <= endPage; i++) {
        pages.push(
            <button
                key={i}
                onClick={() => onPageChange(i)}
                className={`px-3 py-2 border border-gray-300 ${
                    i === currentPage
                        ? 'bg-indigo-600 text-white'
                        : 'bg-white text-gray-500 hover:bg-gray-50'
                }`}
            >
                {i + 1}
            </button>
        );
    }

    // Последняя страница
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            pages.push(
                <span key="end-ellipsis" className="px-3 py-2 border border-gray-300 bg-gray-50">
                    ...
                </span>
            );
        }
        pages.push(
            <button
                key={totalPages - 1}
                onClick={() => onPageChange(totalPages - 1)}
                className="px-3 py-2 border border-gray-300 bg-white text-gray-500 hover:bg-gray-50"
            >
                {totalPages}
            </button>
        );
    }

    // Кнопка "Вперед"
    if (currentPage < totalPages - 1) {
        pages.push(
            <button
                key="next"
                onClick={() => onPageChange(currentPage + 1)}
                className="px-3 py-2 border border-gray-300 rounded-r-md bg-white text-gray-500 hover:bg-gray-50"
            >
                Next
            </button>
        );
    }

    return (
        <div className={`flex justify-center items-center space-x-0 ${className}`}>
            {pages}
        </div>
    );
};

export default Pagination;