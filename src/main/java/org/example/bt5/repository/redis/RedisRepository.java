package org.example.bt5.repository.redis;

import org.example.bt5.model.BookCache;
import org.springframework.data.repository.CrudRepository;

public interface RedisRepository extends CrudRepository<BookCache, String> {

    boolean existsById(String id);
}
