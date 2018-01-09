/*
 * Copyright (c) 2018 by Stefan Schubert
 */

package de.bluewhale.sabi.persistence.dao;

import de.bluewhale.sabi.persistence.model.EntityState;
import de.bluewhale.sabi.persistence.model.TracableEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Using DAO-Pattern: http://www.codeproject.com/Articles/251166/The-Generic-DAO-pattern-in-Java-with-Spring-3-and <br/>
 * Licence: http://www.codeproject.com/info/cpol10.aspx <br />
 * User: Stefan Schubert <br />
 * Date: 06.09.15
 */
public abstract class GenericDaoImpl<T extends TracableEntity> implements GenericDao<T> {

    @PersistenceContext(unitName = "sabi")
    protected EntityManager em;

    private Class<T> type;

    public GenericDaoImpl() {
        Type t = getClass().getGenericSuperclass();
        ParameterizedType pt = (ParameterizedType) t;
        type = (Class) pt.getActualTypeArguments()[0];
    }

    @Override
    public long countAll(final Map<String, Object> params) {

        // TODO StS 06.09.15: Fix this method
        // queryString.append(this.getQueryClauses(params, null));

        final Query query = this.em.createQuery("SELECT count(o) from " + type.getSimpleName() + " o ");
        return (Long) query.getSingleResult();

    }

    @Override
    public T create(final T t) {
        t.setEntityState(new EntityState());
        t.getEntityState().setCreatedOn(LocalDateTime.now());
        this.em.persist(t);
        em.flush(); // because we want to return the auto generated id.
        return t;
    }

    @Override
    public void delete(final Object id) {
        this.em.remove(this.em.getReference(type, id));
    }

    @Override
    public T find(final Object id) {
        return (T) this.em.find(type, id);
    }

    @Override
    public T update(final T t) {
        t.getEntityState().setLastmodOn(LocalDateTime.now());
        this.em.merge(t);
        return t;
    }
}