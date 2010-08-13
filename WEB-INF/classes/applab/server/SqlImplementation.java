package applab.server;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import applab.surveys.server.DatabaseHelpers;

/**
 * class to abstract SQL implementation so that we can polymorphically switch between local and remote
 * database access
 *
 */
public abstract class SqlImplementation {
    static SqlImplementation current;
    
    protected SqlImplementation() {
    }

    public static SqlImplementation getCurrent() {
        if (SqlImplementation.current == null) {
            SqlImplementation.current = new LocalSqlImplementation();
        }
        
        return SqlImplementation.current;
    }
    
    // used by test code to override our default implementation
    public static void setCurrent(SqlImplementation value) {
        SqlImplementation.current = value;
    }

    public abstract Instance createInstance();
    
    public abstract class Instance {
        public abstract ResultSet executeQuery(DatabaseId database, String commandText) throws ClassNotFoundException, SQLException;
        public abstract void dispose() throws SQLException;
    }
    
    private static class LocalSqlImplementation extends SqlImplementation {
        public LocalSqlImplementation() {
            super();            
        }

        @Override
        public Instance createInstance() {
            return new LocalSqlInstance();
        }
        
        private class LocalSqlInstance extends Instance {
            Connection connection;
            Statement statement;

            @Override
            public void dispose() throws SQLException {
                if (this.statement != null) {
                    this.statement.close();
                }

                if (this.connection != null) {
                    this.connection.close();
                }
            }

            @Override
            public ResultSet executeQuery(DatabaseId database, String commandText) throws ClassNotFoundException, SQLException {
                this.connection = DatabaseHelpers.createReaderConnection(database);
                this.statement = this.connection.createStatement();
                return this.statement.executeQuery(commandText);
            }
        }
    }
}
