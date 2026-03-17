package org.example.bt4.repository.redis;

import org.example.bt4.model.Book;
import org.springframework.data.repository.CrudRepository;

public interface RedisRepository extends CrudRepository<Book, Long> {

}
