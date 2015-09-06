package de.bluewhale.sabi.persistence.dao;


import java.util.Map;

/**
 * Using DAO-Pattern: http://www.codeproject.com/Articles/251166/The-Generic-DAO-pattern-in-Java-with-Spring-3-and <br/>
 * Licence: http://www.codeproject.com/info/cpol10.aspx <br />
 * User: Stefan Schubert <br />
 * Date: 06.09.15
 */
public interface GenericDao<T> {

    /**
     * Method that returns the number of entries from a table that meet some
     * criteria (where clause params)
     *
     * @param params sql parameters
     * @return the number of records meeting the criteria
     */
    long countAll(Map<String, Object> params);

    T create(T t);

    void delete(Object id);

    T find(Object id);

    T update(T t);

}
