/*
 * This file is part of MML.
 *
 *  MML is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  MML is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MML.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */
package calliope.core.database;

import calliope.core.image.MimeType;
import java.awt.Rectangle;
import calliope.core.constants.Database;
import calliope.core.exception.DbException;
import java.util.HashSet;
/**
 * Abstract database API for various databases/repositories
 * @author desmond
 */
public abstract class Connection 
{
    String user;
    String password;
    String host;
    int dbPort;
    int wsPort;
    String webRoot;
    protected String databaseName;
    public Connection( String user, String password, String host, 
        String dbName, int dbPort, int wsPort, String webRoot )
    {
        this.user = user;
        this.password = password;
        this.host = host;
        this.dbPort = dbPort;
        this.wsPort = wsPort;
        this.databaseName = dbName;
        this.webRoot = webRoot;
    }
    public final int getDbPort()
    {
        return dbPort;
    }
    public final int getWsPort()
    {
        return wsPort;
    }
    public final String getHost()
    {
        return host;
    }
    /**
     * Remove the rightmost segment of the path and resource
     * @return the remains of the path
     */
    public static String chomp( String path )
    {
        String popped = "";
        int index = path.lastIndexOf( "/" );
        if ( index != -1 )
            popped = path.substring( 0, index );
        return popped;
    }
    public abstract String[] listCollection( String coll ) 
        throws DbException;
    public abstract String[] listCollectionByKey( String collName, String key ) 
        throws DbException;
    public abstract String[] listDocuments( String coll, String expr, String key )
        throws DbException;
    public abstract String getFromDb( String coll, String docID ) 
        throws DbException;
    public abstract String getFromDb( String coll, String dbase, String docID, String version )
        throws DbException;
    public abstract String getFromDbByField( String coll, String value, String field ) 
        throws DbException;
    public abstract String putToDb( String coll, String docID, String json ) 
        throws DbException;
    public abstract String putToDb( String collName, String dbase, 
        String docid, String version, String json ) throws DbException;
    public abstract String addToDb( String collName, String json ) throws DbException;
    public abstract String removeFromDb( String collName, String dbase, String docID, 
        String version ) throws DbException;
    public abstract String removeFromDb( String coll, String docID ) 
        throws DbException;
    public abstract String removeFromDbByField( String collName, String field, 
        String value ) throws DbException;
    public abstract String getMetadata( String docID );
    public abstract String removeFromDbByExpr( String coll, String field, 
        String expr ) throws DbException;
    public abstract void  updateByField( String coll, String findField, 
        Object findValue, String setField, Object setValue ) throws DbException;
}
