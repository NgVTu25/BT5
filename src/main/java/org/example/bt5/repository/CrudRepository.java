package org.example.bt5.repository;

import java.util.List;

public interface CrudRepository<T, ID> {
    void saveBook(T book);

    void updateBook(ID id, T book);

    void deleteBooks(List<ID> ids);

    void saveAll(List<T> books);
}
