package cn.enn.portal.jobCenter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncRepositorySaver<Entity> implements Runnable {

    private static ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private Logger logger = LoggerFactory.getLogger(AsyncRepositorySaver.class);

    private JpaRepository<Entity, Integer> repository;
    private Entity entity;

    private AsyncRepositorySaver(JpaRepository<Entity, Integer> repository, Entity entity) {
        this.repository = repository;
        this.entity = entity;
    }


    @Override
    public void run() {
        try {
            this.repository.save(this.entity);
        } catch (Exception ex) {
            logger.error("save entity error", ex);
        }
    }

    public static <Entity> void AsyncSave(JpaRepository<Entity, Integer> repository, Entity entity) {
        AsyncRepositorySaver<Entity> saver = new AsyncRepositorySaver<>(repository, entity);
        threadPool.submit(saver);
    }
}
