package org.example.bt5.repository;

public interface BookRepository<T, ID> extends CrudRepository<T, ID>, PagingAndSortingRepository<T, ID> {

}
