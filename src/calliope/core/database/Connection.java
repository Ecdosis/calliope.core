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

import calliope.core.exception.DbException;
import java.awt.Rectangle;
import calliope.core.exception.DbException;

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
    
    public Connection( String user, String password, String host, 
        int dbPort, int wsPort )
    {
        this.user = user;
        this.password = password;
        this.host = host;
        this.dbPort = dbPort;
        this.wsPort = wsPort;
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
     * A docID is not allowed if there is already a file in its parent "dir"
     * @throws a PathException if that is not the case
     */
    protected void docIDCheck( String coll, String docID ) throws DbException
    {
        try
        {
            String parent = chomp(docID);
            if ( getFromDb(coll,parent)!=null )
                throw new DbException("ambiguous path "+docID);
        }
        catch ( Exception e )
        {
            throw new DbException(e);
        }
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
    public abstract String[] listDocuments( String coll, String expr )
        throws DbException;
    public abstract String getFromDb( String coll, String docID ) 
        throws DbException;
    public abstract String putToDb( String coll, String docID, String json ) 
        throws DbException;
    public abstract String removeFromDb( String coll, String docID ) 
        throws DbException;
    public abstract byte[] getImageFromDb( String coll, String docID, MimeType type ) 
        throws DbException;
    public abstract String getMetadata( String coll, String docID );
    public abstract void putImageToDb( String coll, String docID, byte[] data, 
        int width, int height, String mimeType ) throws DbException;
    public abstract void removeImageFromDb( String coll, String docID ) 
        throws DbException;
    public abstract Rectangle getImageDimensions( String coll, String docID, 
        MimeType type );
}
