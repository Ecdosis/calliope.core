/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package calliope.core.handler;

import calliope.core.database.Connector;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import calliope.core.exception.CalliopeException;
import calliope.core.constants.JSONKeys;
import calliope.core.constants.Formats;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import calliope.core.Utils;

/**
 * Superclass that has basic in/out database functions for MVD etc
 * @author desmond
 */
public class GetHandler {
    /**
     * Try to retrieve the CorTex/CorCode version specified by the path
     * @param db the database to fetch from
     * @param docID the document ID
     * @param vPath the groups/version path to get or null for default version
     * @return the CorTex/CorCode version contents or null if not found
     * @throws CalliopeException if the resource couldn't be found for some reason
     */
    protected AeseVersion doGetResourceVersion( String db, String docID, 
        String vPath ) throws CalliopeException
    {
        AeseVersion version = new AeseVersion();
        JSONObject doc = null;
        byte[] data = null;
        String res = null;
        //System.out.println("fetching version "+vPath );
        try
        {
            res = Connector.getConnection().getFromDb(db,docID);
        }
        catch ( Exception e )
        {
            throw new CalliopeException( e );
        }
        if ( res != null )
            doc = (JSONObject)JSONValue.parse( res );
        if ( doc != null )
        {
            String format = (String)doc.get(JSONKeys.FORMAT);
            if ( format == null )
                throw new CalliopeException("doc missing format");
            version.setFormat( format );
            // first resolve the link, if any
            if ( version.getFormat().equals(Formats.LINK) )
            {
                data = AeseLink.readLink( doc, version.getContentFormat() );
                version.setVersion( data );
            }
            else if ( version.getFormat().equals(Formats.MVD) )
            {
                MVD mvd = MVDFile.internalise( (String)doc.get(
                    JSONKeys.BODY) );
                if ( vPath == null )
                    vPath = (String)doc.get( JSONKeys.VERSION1 );
                version.setStyle((String)doc.get(JSONKeys.STYLE));
                String sName = Utils.getShortName(vPath);
                String gName = Utils.getGroupName(vPath);
                int vId = mvd.getVersionByNameAndGroup(sName, gName );
                version.setMVD(mvd);
                if ( vId != 0 )
                {
                    data = mvd.getVersion( vId );
                    String desc = mvd.getDescription();
                    //System.out.println("description="+desc);
                    int nversions = mvd.numVersions();
                    //System.out.println("nversions="+nversions);
                    //System.out.println("length of version "+vId+"="+data.length);
                    if ( data != null )
                        version.setVersion( data );
                    else
                        throw new CalliopeException("Version "+vPath+" not found");
                }
                else
                    throw new CalliopeException("Version "+vPath+" not found");
            }
            else
            {
                String body = (String)doc.get( JSONKeys.BODY );
                version.setStyle((String)doc.get(JSONKeys.STYLE));
                if ( body == null )
                    throw new CalliopeException("empty body");
                try
                {
                    data = body.getBytes("UTF-8");
                }
                catch ( Exception e )
                {
                    throw new CalliopeException( e );
                }
                version.setVersion( data );
            }
        }
        return version;
    }
}
