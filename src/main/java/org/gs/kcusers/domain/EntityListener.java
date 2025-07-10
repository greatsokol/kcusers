package org.gs.kcusers.domain;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.gs.kcusers.repositories.AuditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Component
public class EntityListener {
    //Logger logger = LoggerFactory.getLogger(EntityListener.class.getName());

    @Autowired
    @Lazy
    AuditRepository auditRepository;

//    @PrePersist
//    private void preInsert(Object entity) {
//        logger.info("[ENTITY PRE-INSERT] {} about to add an entity {}", getAuthorizedUserOrServiceName(), entity.toString());
//    }

    @PostPersist
    @Transactional(propagation = REQUIRES_NEW)
    public void postInsert(Object entity) {
        //logger.info("[ENTITY POST-INSERT] {} added an entity {}", getAuthorizedUserOrServiceName(), entity.toString());
        auditRepository.save(new Audit(Audit.SUBTYPE_INSERT, entity));
    }

//    @PreUpdate
//    private void preUpdate(Object entity) {
//        logger.info("[ENTITY PRE-UPDATE] {} about to update an entity {}", getAuthorizedUserOrServiceName(), entity.toString());
//    }

    @PostUpdate
    @Transactional(propagation = REQUIRES_NEW)
    public void postUpdate(Object entity) {
        //logger.info("[ENTITY POST-UPDATE] {} updated an entity {}", getAuthorizedUserOrServiceName(), entity.toString());
        auditRepository.save(new Audit(Audit.SUBTYPE_UPDATE, entity));
    }

//    @PreRemove
//    private void preRemove(Object entity) {
//        logger.info("[ENTITY PRE-REMOVE] {} about to remove an entity {}", getAuthorizedUserOrServiceName(), entity.toString());
//    }

    @PostRemove
    @Transactional(propagation = REQUIRES_NEW)
    public void postRemove(Object entity) {
        //logger.info("[ENTITY POST-REMOVE] {} removed an entity {}: ", getAuthorizedUserOrServiceName(), entity.toString());
        auditRepository.save(new Audit(Audit.SUBTYPE_DELETE, entity));
    }

    //@PostLoad
    //private void afterLoad(Object entity) {
    //logger.info("[ENTITY LOAD] {} loaded from database: {}", getAuthorizedUserOrServiceName(), entity.toString());
    //}
}
