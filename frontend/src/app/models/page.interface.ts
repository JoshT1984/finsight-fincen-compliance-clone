export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    // ...other fields as needed
  };
  totalPages: number;
  totalElements: number;
  number: number; // current page index (0-based)
  size: number; // page size
}
