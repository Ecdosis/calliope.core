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
import com.mongodb.WriteConcern;
import com.mongodb.DBCursor;
import org.json.simple.*;
import com.mongodb.WriteResult;

import java.util.regex.Pattern;


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
        String dbName, int dbPort, int wsPort, String webRoot )
    {
        super( user, password, host, dbName, dbPort, wsPort, webRoot );
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
    DBObject getThreeFieldQuery( String field1, String value1, String field2, 
        String value2, String field3, String value3 )
    {
        DBObject query;
        if ( field1.equals(JSONKeys._ID) )
        {
            ObjectId objId = new ObjectId(value1);
            query = new BasicDBObject( field1, objId );
        }
        else
            query = new BasicDBObject(field1,value1);
        if ( field2.equals(JSONKeys._ID) )
        {
            ObjectId objId = new ObjectId(value2);
            query.put(field2, objId );
        }
        else
            query.put(field2,value2);
        if ( field3.equals(JSONKeys._ID) )
        {
            ObjectId objId = new ObjectId(value3);
            query.put(field3, objId );
        }
        else
            query.put(field3,value3);
        return query;
    }
    /**
     * Fetch a resource from the server via a given field value
     * @param collName the collection or database name
     * @param field1 the first field name
     * @param value1 the value of the first field
     * @param field2 the second field name
     * @param value2 the value of the second field
     * @param field3 its field name
     * @param value3 the third value
     * @return the response as a string or null if not found
     */
    private String getFromDbByThreeFields( String collName, String field1, 
        String value1, String field2, String value2, String field3, 
        String value3 ) throws DbException
    {
        try
        {
            connect();
            DBCollection coll = getCollectionFromName( collName );
            DBObject query = getThreeFieldQuery( field1, value1, field2, 
                value2, field3, value3 );
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
     * Fetch a resource from the server, or try to.
     * @param collName the collection or database name
     * @param dbase the database to which this record ultimately belongs
     * @param docID the path to the resource in the collection
     * @param version the version of the scratch resource
     * @return the response as a string or null if not found
     */
    @Override
    public String getFromDb( String collName, String dbase, String docID, 
        String version ) throws DbException
    {
        return getFromDbByThreeFields( collName, JSONKeys.DBASE, dbase, 
            JSONKeys.DOCID, docID, JSONKeys.VERSION1, version );
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
     * PUT a json file to the database using dbase, docid and version
     * @param collName the name of the collection
     * @param dbase the name of the database 
     * @param docid the document identifier
     * @param version the version of the document
     * @param json the json to put there
     * @return the server response
     */
    @Override
    public String putToDb( String collName, String dbase, 
        String docid, String version, String json ) throws DbException
    {
        try
        {
            DBObject doc = (DBObject) JSON.parse(json);
            doc.put( JSONKeys.DOCID, docid );
            connect();
            DBObject query = getThreeFieldQuery(JSONKeys.DBASE,dbase,
                JSONKeys.DOCID,docid,JSONKeys.VERSION1, version);
            DBCollection coll = getCollectionFromName( collName );
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
    private String removeFromDbByThreeFields( String collName, String field1, 
        String value1, String field2, String value2, String field3, 
        String value3 ) throws DbException
    {
        try
        {
            connect();
            DBCollection coll = getCollectionFromName( collName );
            DBObject query = getThreeFieldQuery(field1,value1,field2,value2,field3,value3);
            WriteResult result = coll.remove( query );
            if ( result != null )
                return result.toString();
            else
                return null;
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
    public String removeFromDb( String collName, String dbase, String docID, 
        String version ) throws DbException
    {
        return removeFromDbByThreeFields( collName, JSONKeys.DBASE, dbase,
            JSONKeys.DOCID, docID, JSONKeys.VERSION1, version );
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
                DBObject dbObj = iter.next();
                Object obj = dbObj.get( key );
                if ( key.equals(JSONKeys._ID) )
                {
                    ObjectId id = (ObjectId)dbObj.get(JSONKeys._ID);
                    obj = id.toStringMongod();
                    docs[i++] = (String)obj;
                }
                else
                    docs[i++] = obj.toString();
            }
            return docs;
        }
        else
            return new String[0];
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
    /**
     * Get the metadata as a string
     * @param docid its filename or docid
     * @return the metadata of the document as a string
     */
    public String getMetadata( String docid )
    {
        try
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
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            return null;
        }
    }
    /**
     * Update one field of a database document
     * @param coll the collection the document is in
     * @param findField the field to look for
     * @param findValue the field value to search for
     * @param setField the field to set
     * @param setValue the new field value
     * @throws DbException 
     */
    public void updateByField( String coll, String findField, 
        Object findValue, String setField, Object setValue ) throws DbException
    {
        try
        {
            connect();
            DBCollection collection = getCollectionFromName( coll );
            if ( findField.equals(JSONKeys._ID) )
                findValue = new ObjectId((String)findValue);
            BasicDBObject update = new BasicDBObject(
                "$set", new BasicDBObject(setField,setValue));
            BasicDBObject query = new BasicDBObject();
            query.put(findField,findValue);
            WriteResult res = collection.update(query, update);
            System.out.println(res);
        }
        catch ( Exception e )
        {
            throw new DbException(e);
        }
    }
}