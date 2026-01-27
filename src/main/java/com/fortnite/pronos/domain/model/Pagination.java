package com.fortnite.pronos.domain.model;

/**
 * Domain-specific pagination DTO (no framework dependencies).
 * Replaces Spring's Pageable to maintain domain layer purity.
 */
public class Pagination {
  private final int page;
  private final int size;
  private final String sortBy;
  private final String sortDirection;

  public Pagination(int page, int size, String sortBy, String sortDirection) {
    this.page = page;
    this.size = size;
    this.sortBy = sortBy != null ? sortBy : "createdAt";
    this.sortDirection = sortDirection != null ? sortDirection : "DESC";
  }

  public static Pagination of(int page, int size) {
    return new Pagination(page, size, "createdAt", "DESC");
  }

  public static Pagination of(int page, int size, String sortBy, String sortDirection) {
    return new Pagination(page, size, sortBy, sortDirection);
  }

  public int getPage() {
    return page;
  }

  public int getSize() {
    return size;
  }

  public String getSortBy() {
    return sortBy;
  }

  public String getSortDirection() {
    return sortDirection;
  }

  @Override
  public String toString() {
    return "Pagination{" +
        "page=" + page +
        ", size=" + size +
        ", sortBy='" + sortBy + '\'' +
        ", sortDirection='" + sortDirection + '\'' +
        '}';
  }
}
