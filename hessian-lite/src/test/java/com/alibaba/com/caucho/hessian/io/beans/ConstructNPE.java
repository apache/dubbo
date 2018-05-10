package com.alibaba.com.caucho.hessian.io.beans;

import java.beans.PropertyChangeEvent;
import java.sql.SQLException;

/**
 * <a href="https://github.com/apache/incubator-dubbo/issues/210">#210</a>
 * @see org.springframework.jdbc.UncategorizedSQLException
 * @see org.springframework.beans.PropertyAccessException
 */
public class ConstructNPE extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String sql;
    private transient PropertyChangeEvent propertyChangeEvent;

    public ConstructNPE(String task, String sql, SQLException ex, PropertyChangeEvent propertyChangeEvent) {
        super(task + "; uncategorized SQLException for SQL [" + sql + "]; SQL state ["
                + ex.getSQLState() + "]; error code [" + ex.getErrorCode() + "]; " + ex.getMessage()
                + propertyChangeEvent.getPropertyName(), ex);
        this.sql = sql;
        this.propertyChangeEvent = propertyChangeEvent;
    }

    public SQLException getSQLException() {
        return (SQLException) getCause();
    }

    public String getSql() {
        return this.sql;
    }

    public PropertyChangeEvent getPropertyChangeEvent() {
        return this.propertyChangeEvent;
    }
}
