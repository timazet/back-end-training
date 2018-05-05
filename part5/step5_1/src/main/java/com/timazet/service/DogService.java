package com.timazet.service;

import com.timazet.controller.DogNotFoundException;
import com.timazet.controller.dto.Dog;
import com.timazet.dao.DogDao;
import com.timazet.dao.JdbcConnectionHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class DogService {

    private final DogDao dao;
    private final JdbcConnectionHolder connectionHolder;

    public Collection<Dog> get() {
        return execute(dao::get);
    }

    public Dog get(UUID id) {
        Assert.notNull(id, "Id should not be null");

        Dog result = execute(() -> dao.get(id));
        if (result == null) {
            throw new DogNotFoundException(id);
        }
        return result;
    }

    public Dog create(Dog dog) {
        Assert.notNull(dog, "Dog should not be null");

        dog.setId(UUID.randomUUID());
        execute(() -> dao.create(dog));
        return dog;
    }

    public Dog update(Dog dog) {
        Assert.notNull(dog, "Dog should not be null");
        Assert.notNull(dog.getId(), "Id should not be null");

        if (execute(() -> dao.update(dog)) <= 0) {
            throw new DogNotFoundException(dog.getId());
        }
        return dog;
    }

    public void delete(UUID id) {
        Assert.notNull(id, "Id should not be null");

        if (execute(() -> dao.delete(id)) <= 0) {
            throw new DogNotFoundException(id);
        }
    }

    private <T> T execute(final Supplier<T> supplier) {
        try {
            Connection connection = connectionHolder.getConnection();
            connection.setAutoCommit(false);
            try {
                T result = supplier.get();
                connection.commit();
                return result;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            connectionHolder.closeConnection();
        }
    }

}
