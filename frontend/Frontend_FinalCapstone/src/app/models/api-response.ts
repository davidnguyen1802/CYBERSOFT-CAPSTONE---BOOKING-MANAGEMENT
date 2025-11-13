/**
 * Generic API Response wrapper
 * Used for paginated responses
 */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

/**
 * Paginated response content
 */
export interface PageContent<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}
