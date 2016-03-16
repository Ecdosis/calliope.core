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

package calliope.core.handler;

import calliope.core.database.Connector;
import calliope.core.constants.Database;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import calliope.core.exception.CalliopeException;
import calliope.core.constants.JSONKeys;
import calliope.core.constants.Formats;
import calliope.core.constants.HTMLNames;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import calliope.core.Utils;
import calliope.exception.AeseException;
import calliope.json.corcode.Range;
import calliope.json.corcode.STILDocument;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Superclass that has basic in/out database functions for MVD etc
 * @author desmond
 */
public class GetHandler extends Handler
{
    protected JSONObject doGetMetadata( String docid ) 
        throws CalliopeException
    {
        String res = null;
        JSONObject jDoc = null;
        try
        {
            res = Connector.getConnection().getFromDb(Database.METADATA,docid);
            if ( res != null )
                jDoc = (JSONObject)JSONValue.parse( res );
            return jDoc;
        }
        catch ( Exception e )
        {
            throw new CalliopeException( e );
        }
    }

    protected EcdosisMVD doGetMVD( String db, String docid ) 
        throws CalliopeException
    {
        String res = null;
        JSONObject jDoc = null;
        try
        {
            res = Connector.getConnection().getFromDb(db,docid);
            if ( res != null )
                jDoc = (JSONObject)JSONValue.parse( res );
            if ( jDoc != null )
            {
                String format = (String)jDoc.get(JSONKeys.FORMAT);
                if ( format != null && format.contains(Formats.MVD) )
                {
                    return new EcdosisMVD(jDoc);
                }
            }
            return null;
        }
        catch ( Exception e )
        {
            throw new CalliopeException( e );
        }
    }
    /**
     * Try to retrieve the CorTex/CorCode version specified by the path
     * @param db the database to fetch from
     * @param docID the document ID
     * @param vPath the groups/version path to get or null for default version
     * @return the CorTex/CorCode version contents or null if not found
     * @throws CalliopeException if the resource couldn't be found for some reason
     */
    protected EcdosisVersion doGetResourceVersion( String db, String docID, 
        String vPath ) throws CalliopeException
    {
        EcdosisVersion version = new EcdosisVersion();
        JSONObject doc = null;
        char[] data = null;
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
                data = body.toCharArray();
                version.setVersion( data );
            }
        }
        return version;
    }
    /**
     * Make a slash-delimited version id
     * @param shortName the short name of the version
     * @param groupPath an array of group names in order
     * @return a slash-delimited string containing all starting with "/"
     */
    static String makeVersionId( String shortName, ArrayList<String> groupPath )
    {
        StringBuilder sb = new StringBuilder();
        sb.append("/" );
        for ( int i=0;i<groupPath.size();i++ )
        {
            sb.append( groupPath.get(i) );
            sb.append("/");
        }
        sb.append( shortName );
        return sb.toString();
    }
    /**
     * Annotate a raw text table with standoff properties suitable for a list
     * @param table the raw text table returned by nmerge
     * @param listName the NAME of the list in HTML
     * @param listId the id of the select list
     * @param version1 the selected version
     * @return the markup of the list
     * @throws AeseException 
     */
    public static String markupVersionTable( String table, String listName, 
        String listId, String version1 ) throws AeseException
    {
        STILDocument doc = new STILDocument();
        String[] lines = table.split("\n");
        if ( lines.length > 0 )
        {
            ArrayList<Range> groups = new ArrayList<Range>();
            ArrayList<String> groupPath = new ArrayList<String>();
            int offset = lines[0].length()+1;
            Range r = new Range( JSONKeys.DESCRIPTION, 0, lines[0].length() );
            if ( listId != null && listId.length()>0 )
                r.addAnnotation( JSONKeys.ID, listId );
            doc.add( r );
            JSONObject group = null;
            int groupEnd = 0;
            int listStart = offset;
            Range list = new Range( JSONKeys.LIST, offset, 0 );
            list.addAnnotation( JSONKeys.NAME, listName );
            list.addAnnotation( JSONKeys.ID, listName );
            JSONObject listDoc = doc.add( list );
            //int numTopGroups = countTopGroups( lines );
            for ( int i=1;i<lines.length;i++ )
            {
                String[] cols = lines[i].split("\t");
                if ( cols.length > 2 )
                {
                    for ( int j=0;j<cols.length-2;j++ )
                    {
                        // find groups
                        if ( cols[j].equals("top") )
                        {
                            // treat as empty
                            Range removed = new Range( JSONKeys.EMPTY, offset, 
                                cols[j].length()+1 );
                            removed.removed = true;
                            doc.add( removed );
                            groupPath.clear();
                            if ( group != null )
                            {
                                Range h = groups.get(j);
                                int groupLen = groupEnd-h.offset;
                                //System.out.println("groupLen="+groupLen);
                                group.put(JSONKeys.LEN, new Integer(groupLen));
                                // start again
                                group = null;
                                groups.clear();
                            }
                        }
                        else if ( cols[j].length()>0 )
                        {
                            // group names will be attributes not content
                            Range removed = new Range( JSONKeys.EMPTY, offset, 
                                cols[j].length()+1 );
                            removed.removed = true;
                            doc.add( removed );
                            // initial length of 0
                            Range g = new Range( /*(j==0&&numTopGroups==1)?
                                JSONKeys.TOPGROUP:*/JSONKeys.GROUP, 
                                offset+cols[j].length()+1, 0 );
                            g.addAnnotation( JSONKeys.NAME, cols[j] );
                            // set the length of the old group 
                            if ( group != null )
                            {
                                Range h = groups.get(j);
                                int groupLen = groupEnd-h.offset;
                                //System.out.println("groupLen="+groupLen);
                                group.put(JSONKeys.LEN,groupLen);
                            }
                            // if a group is already defined in 
                            // the current last position, remove it
                            if ( groups.size()-1==j )
                                groups.remove( j );
                            groups.add( g );
                            // set current group
                            group = doc.add( g );
                            // update group path
                            if ( j<groupPath.size() )
                                groupPath.set( j, cols[j] );
                            else
                                groupPath.add( cols[j] );
                        }
                        offset += cols[j].length()+1;
                    }
                    // pentultimate column = version short name
                    String versionId = makeVersionId(cols[cols.length-2],
                        groupPath);
                    Range shortName = new Range( JSONKeys.VERSION_SHORT, 
                        offset, cols[cols.length-2].length() );
                    if ( version1 != null && versionId.equals(version1) )
                        shortName.addAnnotation(HTMLNames.SELECTED,
                        HTMLNames.SELECTED);
                    shortName.addAnnotation( JSONKeys.VERSION1, versionId );
                    // ultimate column = version description = long name
                    shortName.addAnnotation( JSONKeys.DESCRIPTION, 
                        cols[cols.length-1] ); 
                    doc.add( shortName );
                    // move past short name
                    offset += cols[cols.length-2].length()+1; // short name+\t
                    // erase long name
                    Range longName = new Range( JSONKeys.EMPTY, 
                        offset, cols[cols.length-1].length() );
                    longName.removed = true;
                    doc.add( longName );
                    // move past long name/description
                    offset += cols[cols.length-1].length()+1; // long name+\n
                    // group end excludes line-feed
                    groupEnd = offset-1;
                }
                else
                {
                    throw new AeseException("ill-formed group/version record");
                }
            }
            if ( group != null && groups.size() > 0 )
            {
                Range h = groups.get(groups.size()-1);
                group.put(JSONKeys.LEN,new Integer(groupEnd-h.offset));
            }
           // else
             //   throw new AeseException("no groups defined");
            // update list length
            listDoc.put( JSONKeys.LEN, new Integer(offset-listStart) );
        }
        else
            throw new AeseException( "invalid version table: no CRs");
        return doc.toString();
    }
    /**
     * Subclass must implement
     * @param request the http request
     * @param response the http response
     * @param urn the residual urn
     * @throws CalliopeException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws CalliopeException
    {
    }
}
