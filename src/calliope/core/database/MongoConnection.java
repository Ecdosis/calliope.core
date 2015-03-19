/* This file is part of calliope.
 *
 *  calliope is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  calliope is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with calliope.  If not, see <http://www.gnu.org/licenses/>.
 */
package calliope.core.database;

import calliope.core.image.MimeType;
import calliope.core.exception.*;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import java.util.Iterator;
import java.util.ArrayList;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.util.JSON;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.WriteConcern;
import com.mongodb.DBCursor;
import org.json.simple.*;

import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.regex.Pattern;
import java.util.List;
import java.util.HashSet;
import java.awt.Rectangle;


/**
 * Database interface for MongoDB
 * @author desmond
 */
public class MongoConnection extends Connection 
{
    MongoClient client;
    static int MONGO_PORT = 27017;
    /** connection to database */
    DB  db;
    public MongoConnection( String user, String password, String host, 
        String dbName, int dbPort, int wsPort )
    {
        super( user, password, host, dbName, dbPort, wsPort );
    }
    /**
     * Connect to the database
     * @throws Exception 
     */
    private void connect() throws Exception
    {
        if ( db == null )
        {
            MongoClient mongoClient = new MongoClient( host, MONGO_PORT );
            db = mongoClient.getDB(this.databaseName);
            //boolean auth = db.authenticate( user, password.toCharArray() );
            //if ( !auth )
            //    throw new DbException( "MongoDB authentication failed");
        }
    }
    /**
     * Get the Mongo db collection object from its name
     * @param collName the collection name
     * @return a DBCollection object
     * @throws DbException 
     */
    private DBCollection getCollectionFromName( String collName )
        throws DbException
    {
        DBCollection coll = db.getCollection( collName );
        if ( coll == null )
            coll = db.createCollection( collName, null );
        if ( coll != null )
            return coll;
        else
            throw new DbException( "Unknown collection "+collName );
    }
    /**
     * Fetch a resource from the server via a given field value
     * @param collName the collection or database name
     * @param value the value of the field
     * @param field the field name
     * @return the response as a string or null if not found
     */
    @Override
    public String getFromDbByField( String collName, String value, String field ) 
        throws DbException
    {
        try
        {
            connect();
            DBCollection coll = getCollectionFromName( collName );
            DBObject  query;
            if ( field.equals(JSONKeys._ID) )
            {
                ObjectId objId = new ObjectId(value);
                query = new BasicDBObject( field, objId );
            }
            else
                query = new BasicDBObject(field,value);
            DBObject obj = coll.findOne( query );
            if ( obj != null )
                return obj.toString();
            else
                return null;
        }
        catch ( Exception e )
        {
            throw new DbException( e );
        }
    }
    /**
     * Fetch a resource from the server, or try to.
     * @param collName the collection or database name
     * @param docID the path to the resource in the collection
     * @return the response as a string or null if not found
     */
    @Override
    public String getFromDb( String collName, String docID ) throws DbException
    {
        return getFromDbByField( collName,docID, JSONKeys.DOCID );
    }
    /**
     * PUT a json file to the database
     * @param collName the name of the collection
     * @param docID the docid of the resource 
     * @param json the json to put there
     * @return the server response
     */
    @Override
    public String putToDb( String collName, String docID, String json ) 
        throws DbException
    {
        try
        {
            docIDCheck( collName, docID );
            DBObject doc = (DBObject) JSON.parse(json);
            doc.put( JSONKeys.DOCID, docID );
            connect();
            DBCollection coll = getCollectionFromName( collName );
            DBObject query = new BasicDBObject( JSONKeys.DOCID, docID );
            WriteResult result = coll.update( query, doc, true, false );
            //return removeFromDb( path );
            return result.toString();
        }
        catch ( Exception e )
        {
            throw new DbException( e );
        }
    }
    /**
     * PUT a new json file to the database
     * @param collName the name of the collection
     * @param json the json to put there
     * @return the server response
     */
    @Override
    public String addToDb( String collName, String json ) throws DbException
    {
        try
        {
            DBObject doc = (DBObject) JSON.parse(json);
            connect();
            DBCollection coll = getCollectionFromName( collName );
            if (doc.containsField(JSONKeys._ID) )
            {
                Object id = doc.get(JSONKeys._ID);
                DBObject query = new BasicDBObject( JSONKeys._ID, id );
                if ( query != null )
                {
                    WriteResult result = coll.update( query, doc, true, false );
                    return result.toString();
                }
                else
                    throw new Exception("Failed to update object "+id);
            }
            else
            {
                WriteResult result = coll.insert( doc, WriteConcern.ACKNOWLEDGED );
                // return the new document's id
                ObjectId id = (ObjectId)doc.get( "_id" );
                JSONObject jDoc = (JSONObject)JSONValue.parse(result.toString());
                jDoc.put("_id",id.toString());
                return jDoc.toJSONString();
            }
        }
        catch ( Exception e )
        {
            throw new DbException( e );
        }
    }
    /**
     * Remove a document from the database
     * @param collName name of the collection
     * @param docID the docid of the resource 
     * @param json the json to put there
     * @return the server response
     */
    @Override
    public String removeFromDb( String collName, String docID ) 
        throws DbException
    {
        return removeFromDbByField( collName, JSONKeys.DOCID, docID );
    }
    /**
     * Remove a document from the database by a unique field value
     * @param collName name of the collection
     * @param field the name of the field 
     * @param value the value of the field
     * @return the server response
     */
    @Override
    public String removeFromDbByField( String collName, String field, 
        String value ) throws DbException
    {
        try
        {
            connect();
            DBCollection coll = getCollectionFromName( collName );
            //new BasicDBObject("_id", new ObjectId(idString));
            Object obj = value;
            if ( field.equals(JSONKeys._ID) )
                obj = new ObjectId(value);
            DBObject query = new BasicDBObject( field, obj );
            WriteResult result = coll.remove( query );
            return result.toString();
        }
        catch ( Exception e )
        {
            throw new DbException( e );
        }
    }
    /**
     * Remove an entire set of documents that match a regular expression.
     * @param collName the collection to remove from
     * @param key the key field to match
     * @param expr the regular expression for key's values
     * @return the result
     * @throws DbException 
     */
    public String removeFromDbByExpr( String collName, String key, String expr )
        throws DbException
    {
        try
        {
            DBCollection coll = getCollectionFromName( collName );
            if ( coll != null )
            {
                BasicDBObject q = new BasicDBObject();
                q.put(key, Pattern.compile(expr) );
                WriteResult result = coll.remove( q );
                return result.toString();
            }
            else
                throw new Exception("Collection "+collName+" not found");
        }
        catch ( Exception e )
        {
            throw new DbException(e);
        }
    }
    /**
     * Get a list of docIDs or file names corresponding to the regex expr
     * @param collName the collection to query
     * @param expr the regular expression to match against docid
     * @param key the key to retrieve for each matching document
     * @return an array of matching docids, which may be empty
     */
    @Override
    public String[] listDocuments( String collName, String expr, String key )
        throws DbException
    {
        try
        {
            try
            {
                connect();
            }
            catch ( Exception e )
            {
                throw new DbException( e );
            }
            if ( !collName.equals(Database.CORPIX) )
            {
                DBCollection coll = getCollectionFromName( collName );
                if ( coll != null )
                {
                    BasicDBObject q = new BasicDBObject();
                    q.put(JSONKeys.DOCID, Pattern.compile(expr) );
                    DBCursor curs = coll.find( q );
                    ArrayList<String> docids = new ArrayList<String>();
                    Iterator<DBObject> iter = curs.iterator();
                    int i = 0;
                    while ( iter.hasNext() )
                    {
                        Object kId = iter.next().get(key);
                        if ( kId != null )
                            docids.add( kId.toString() );
                    }
                    String[] array = new String[docids.size()];
                    docids.toArray( array );
                    return array;
                }
                else
                    throw new DbException("collection "+collName+" not found");
            }
            else
            {
                GridFS gfs = new GridFS( db, collName );
                BasicDBObject q = new BasicDBObject();
                q.put(JSONKeys.FILENAME, Pattern.compile(expr) );
                DBCursor curs = gfs.getFileList(q);
                int i = 0;
                List<DBObject> list = curs.toArray();
                HashSet<String> set = new HashSet<String>();
                Iterator<DBObject> iter = list.iterator();
                while ( iter.hasNext() )
                {
                    String name = (String)iter.next().get("filename");
                    set.add(name);
                }
                String[] docs = new String[set.size()];
                set.toArray( docs );
                return docs;
            }
        }
        catch ( Exception e )
        {
            throw new DbException( e );
        }
    }
    /**
     * List all the documents in a Mongo collection
     * @param collName the name of the collection
     * @param key the document key to retrieve by
     * @return a String array of document keys
     * @throws DbException 
     */
    @Override
    public String[] listCollectionByKey( String collName, String key ) 
        throws DbException
    {
        try
        {
            connect();
        }
        catch ( Exception e )
        {
            throw new DbException( e );
        }
        DBCollection coll = getCollectionFromName( collName );
        BasicDBObject keys = new BasicDBObject();
        keys.put( key, 1 );
        DBCursor cursor = coll.find( new BasicDBObject(), keys );
        if ( cursor.length() > 0 )
        {
            String[] docs = new String[cursor.length()];
            Iterator<DBObject> iter = cursor.iterator();
            int i = 0;
            while ( iter.hasNext() )
            {
                Object obj = iter.next().get( key );
                docs[i++] = obj.toString();
            }
            return docs;
        }
        else
            throw new DbException( "no docs in collection "+collName );
    }
    /**
     * List all the documents in a Mongo collection
     * @param collName the name of the collection
     * @return a String array of document keys
     * @throws DbException 
     */
    @Override
    public String[] listCollection( String collName ) throws DbException
    {
        if ( !collName.equals(Database.CORPIX) )
        {
            try
            {
                connect();
            }
            catch ( Exception e )
            {
                throw new DbException( e );
            }
            DBCollection coll = getCollectionFromName( collName );
            BasicDBObject keys = new BasicDBObject();
            keys.put( JSONKeys.DOCID, 1 );
            DBCursor cursor = coll.find( new BasicDBObject(), keys );
            if ( cursor.length() > 0 )
            {
                String[] docs = new String[cursor.length()];
                Iterator<DBObject> iter = cursor.iterator();
                int i = 0;
                while ( iter.hasNext() )
                    docs[i++] = (String)iter.next().get( JSONKeys.DOCID );
                return docs;
            }
            else
                return new String[0];
        }
        else
        {
            GridFS gfs = new GridFS( db, collName );
            DBCursor curs = gfs.getFileList();
            int i = 0;
            List<DBObject> list = curs.toArray();
            HashSet<String> set = new HashSet<String>();
            Iterator<DBObject> iter = list.iterator();
            while ( iter.hasNext() )
            {
                String name = (String)iter.next().get("filename");
                set.add(name);
            }
            String[] docs = new String[set.size()];
            set.toArray( docs );
            return docs;
        }
    }
    /**
     * Get an image from the database
     * @param collName the collection name
     * @param docID the docid of the corpix
     * @return the image data
     */
    @Override
    public byte[] getImageFromDb( String collName, String docID, MimeType type )
    {
        try
        {
            connect();
            GridFS gfs = new GridFS( db, collName );
            GridFSDBFile file = gfs.findOne( docID );
            if ( file != null )
            {
                InputStream ins = file.getInputStream();
                type.mimeType = file.getContentType();
                long dataLen = file.getLength();
                // this only happens if it is > 2 GB
                if ( dataLen > Integer.MAX_VALUE )
                    throw new DbException( "file too big (size="+dataLen+")" );
                byte[] data = new byte[(int)dataLen];
                int offset = 0;
                while ( offset < dataLen )
                {
                    int len = ins.available();
                    offset += ins.read( data, offset, len );
                }
                return data;
            }
            else
                throw new FileNotFoundException(docID);
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            return null;
        }
    }
    /**
     * Get the metadata as a string
     * @param coll the collection
     * @param docid its filename or docid
     * @return the metadata of the document as a string
     */
    public String getMetadata( String coll, String docid )
    {
        try
        {
            if ( !coll.equals(Database.CORPIX) )
            {
                connect();
                DBCollection collection = getCollectionFromName( Database.METADATA );
                DBObject  query;
                query = new BasicDBObject(JSONKeys.DOCID,docid);
                DBObject obj = collection.findOne( query );
                if ( obj != null )
                {
                    obj.removeField(JSONKeys._ID);
                    return obj.toString();
                }
                else
                    return null;
            }
            else
            {
                GridFS gfs = new GridFS( db, coll );
                GridFSDBFile file = gfs.findOne( docid );
                if ( file != null )
                {
                    DBObject obj = file.getMetaData();
                    return obj.toString();
                }
            }
            return null;
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            return null;
        }
    }
    /**
     * Get the image dimensions without loading it
     * @param coll the collection it is stored in
     * @param docID the document identifier
     * @return a Rect containing width and height only or null
     * @param type VAR param for mime type
     */
    public Rectangle getImageDimensions( String coll, String docID, MimeType type )
    {
        try
        {
            connect();
            GridFS gfs = new GridFS( db, coll );
            GridFSDBFile file = gfs.findOne( docID );
            if ( file != null )
            {
                DBObject obj = file.getMetaData();
                Object width = obj.get("width");
                Object height = obj.get("height");
                if ( width != null && height != null )
                {
                    Integer intW = (Integer)width;
                    Integer intH = (Integer)height;
                    Rectangle r = new Rectangle( intW.intValue(), 
                        intH.intValue() );
                    type.mimeType = file.getContentType();
                    return r;
                }
                else
                    return null;
            }
            else
                throw new Exception("file "+docID+" not found");
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            return null;
        }
    }
    /**
     * Store an image in the database
     * @param collName name of the image collection
     * @param docID the docid of the resource
     * @param data the image data to store
     * @param width the width of the image in pixels
     * @param height the height of the image in pixels
     * @param mimeType the type of the image
     * @throws DbException 
     */
    @Override
    public void putImageToDb( String collName, String docID, byte[] data, 
        int width, int height, String mimeType ) throws DbException
    {
        docIDCheck( collName, docID );
        GridFS gfs = new GridFS( db, collName );
        GridFSInputFile	file = gfs.createFile( data );
        file.setFilename( docID );
        BasicDBObject r = new BasicDBObject();
        r.put( "width", width );
        r.put( "height", height );
        file.setMetaData( r );
        file.setContentType(mimeType);
        file.save();
    }
    /**
     * Delete an image from the database
     * @param collName the collection name e.g. "corpix"
     * @param docID the image's docid path
     * @throws DbException 
     */
    @Override
    public void removeImageFromDb( String collName, String docID ) 
        throws DbException
    {
        try
        {
            GridFS gfs = new GridFS( db, collName );
            GridFSDBFile file = gfs.findOne( docID );
            if ( file == null )
                throw new FileNotFoundException("file "+collName+"/"+docID
                    +" not found");
            gfs.remove( file );
        }
        catch ( Exception e )
        {
            throw new DbException( e );
        }
    }
}