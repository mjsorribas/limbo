package org.github.tursodatabase.core;

import java.sql.SQLException;

import org.github.tursodatabase.annotations.NativeInvocation;
import org.github.tursodatabase.annotations.Nullable;
import org.github.tursodatabase.utils.LimboExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * By default, only one <code>resultSet</code> object per <code>LimboStatement</code> can be open at the same time.
 * Therefore, if the reading of one <code>resultSet</code> object is interleaved with the reading of another, each must
 * have been generated by different <code>LimboStatement</code> objects. All execution method in the <code>LimboStatement</code>
 * implicitly close the current <code>resultSet</code> object of the statement if an open one exists.
 */
public class LimboStatement {
    private static final Logger log = LoggerFactory.getLogger(LimboStatement.class);

    private final String sql;
    private final long statementPointer;
    private final LimboResultSet resultSet;

    // TODO: what if the statement we ran was DDL, update queries and etc. Should we still create a resultSet?
    public LimboStatement(String sql, long statementPointer) {
        this.sql = sql;
        this.statementPointer = statementPointer;
        this.resultSet = LimboResultSet.of(this);
        log.debug("Creating statement with sql: {}", this.sql);
    }

    public LimboResultSet getResultSet() {
        return resultSet;
    }

    /**
     * Expects a clean statement created right after prepare method is called.
     *
     * @return true if the ResultSet has at least one row; false otherwise.
     */
    public boolean execute() throws SQLException {
        resultSet.next();
        return resultSet.hasLastStepReturnedRow();
    }

    LimboStepResult step() throws SQLException {
        final LimboStepResult result = step(this.statementPointer);
        if (result == null) {
            throw new SQLException("step() returned null, which is only returned when an error occurs");
        }

        return result;
    }

    @Nullable
    private native LimboStepResult step(long stmtPointer) throws SQLException;

    /**
     * Throws formatted SQLException with error code and message.
     *
     * @param errorCode Error code.
     * @param errorMessageBytes Error message.
     */
    @NativeInvocation(invokedFrom = "limbo_statement.rs")
    private void throwLimboException(int errorCode, byte[] errorMessageBytes) throws SQLException {
        LimboExceptionUtils.throwLimboException(errorCode, errorMessageBytes);
    }

    @Override
    public String toString() {
        return "LimboStatement{" +
               "statementPointer=" + statementPointer +
               ", sql='" + sql + '\'' +
               '}';
    }
}
