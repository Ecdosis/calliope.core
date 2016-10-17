/*
 * This file is part of calliope.core.
 *
 *  calliope.core is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  calliope.core is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with calliope.core.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */

package calliope.core.handler;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import calliope.core.constants.JSONKeys;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
/**
 * Loaded representation of the JSON document representing an MVD 
 * or a single unmerged version of something
 * @author desmond
 */
public class EcdosisMVD 
{
    MVD mvd;
    String version1;
    String format;
    String text;
    String style;
    String description;
    String title;
    String author;
    /**
     * Initialise a AeseMVD object
     * @param doc the JSON document from the database
     */
    public EcdosisMVD( JSONObject doc )
    {
        String body = (String)doc.get(JSONKeys.BODY);
        this.format = (String)doc.get(JSONKeys.FORMAT);
        if ( body != null && format != null )
        {
            if ( format.startsWith("MVD") )
                this.mvd = MVDFile.internalise( body );
            else
                this.text = body;
        }
        this.author = (String)doc.get(JSONKeys.AUTHOR);
        this.description = (String)doc.get(JSONKeys.DESCRIPTION);
        this.style = (String)doc.get(JSONKeys.STYLE);
        this.title = (String)doc.get(JSONKeys.TITLE);
        this.version1 = (String)doc.get(JSONKeys.VERSION1);
        if ( this.version1 == null )
             this.version1 = "/base/layer-final";             
    }
    public String getFormat()
    {
        return format;
    }
    public int numVersions()
    {
        if ( mvd != null )
            return mvd.numVersions();
        else
            return 1;
    }
    public String getVersionName( int vid )
    {
        if ( mvd != null )
            return mvd.getVersionId( (short)vid );
        else 
            return version1;
    }
    public char[] getVersion( int vid )
    {
        if ( mvd != null )
            return mvd.getVersion( vid );
        else if ( text != null )
            return text.toCharArray();
        else
            return null;
    }
    public String getDefaultVersion()
    {
        return this.version1;
    }
    public String getVersionTable()
    {
        if ( this.mvd != null )
            return this.mvd.getVersionTable();
        else
        {
            StringBuilder sb = new StringBuilder();
            if ( this.description != null )
                sb.append(this.description);
            sb.append("\n");
            sb.append("top\t");
            sb.append( version1 );
            sb.append("\tOnly version");
            sb.append("\n");
            return sb.toString();
        }
    }
    public int[] getVersionLengths()
    {
        if ( this.mvd != null )
            return this.mvd.getVersionLengths();
        else
        {
            int[] lengths = new int[1];
            lengths[0] = text.length();
            return lengths;
        }
    }
    public MVD getMVD()
    {
        if ( this.mvd != null )
            return this.mvd;
        else
            return null;
    }
    /**
     * Measure a table without turning it into HTML or JSON
     * @param base the base version
     * @return an array of section starts in base
     */
    public int[] measureTable( short base )
    {
        if ( this.mvd != null )
            return this.mvd.measureTable(base);
        else
        {
            int[] lengths = getVersionLengths();
            int[] stats = new int[2];
            stats[0] = 0;
            stats[1] = text.length();
            return stats;
        }
    }
    /**
     * Get the base version number (1-#versions)
     * @param baseVersion the vid or full version name
     * @param mvd the mvd to get the version of
     * @return the base version without fail (1 by default)
     */
    public short getBaseVersion( String baseVersion )
    {
        if ( this.mvd == null )
            return (short)1;
        else
        {
            String shortName="";
            String groups="";
            if ( baseVersion != null )
            {
                int pos = baseVersion.lastIndexOf("/");
                if ( pos != -1 )
                {
                    shortName = baseVersion.substring(pos+1);
                    groups = baseVersion.substring(0,pos);
                }
                else
                    shortName = baseVersion;
            }
            short base = (short)mvd.getVersionByNameAndGroup( shortName, 
                groups );
            if ( base == 0 )
            {
                System.out.println("version "+shortName+" in group "
                    +groups+" not found. Substituting 1");
                base = 1;
            }
            return base;
        }
    }
    public String getGroupPath( short v2 )
    {
        if ( mvd != null )
            return mvd.getGroupPath(v2);
        else
            return "/";
    }
    public String getVersionShortName( short v2 )
    {
        if ( mvd != null )
            return mvd.getVersionShortName(v2);
        else
            return version1;
    }            
    public int getNextVersionId( short v1 )
    {
        if ( this.mvd != null )
            return this.mvd.getNextVersionId(v1);
        else
            return 1;
    }
    public short getVersionByNameAndGroup( String shortName, 
            String groups )
    {
        if ( this.mvd != null )
            return (short)this.mvd.getVersionByNameAndGroup(shortName,groups);
        else
            return (short)1;
    }
    /**
     * Get a HTML table of the "MVD"
     * @param base the version to regard as the base
     * @param start the start offset within base of the range
     * @param len the length of the range within base
     * @param compact compact the table by merging nearly equal versions
     * @param hideMerged display only base version in merged sections
     * @param wholeWords expand differences to whole words
     * @param spec a specification of a comma-separated set of versions
     * @param firstID ID of the first merged text ID
     * @return a HTML fragment 
     */
    public String getTableView( short base, int start, int len, 
        boolean compact, boolean hideMerged, boolean wholeWords,
        String spec, int firstID, String tableId )
    {
        if ( this.mvd != null )
            return this.mvd.getTableView( base, start, len, 
                compact, hideMerged, wholeWords,
                spec, firstID, tableId );
        else
        {
            // make a one-line table - pretty silly
            StringBuilder sb = new StringBuilder();
            sb.append("<table id=\"");
            sb.append(tableId);
            sb.append("\"><tr>");
            sb.append("<td class=\"siglumleft\">");
            sb.append(this.version1);
            sb.append("</td>");
            sb.append("<td>");
            sb.append(this.text);
            sb.append("</td>");
            sb.append("</tr></table>");
            return sb.toString();
        }
    }
    /**
     * Get a JSON representation of the entire MVD as a table
     * @param base the version to regard as the base
     * @param start the offset into base to start from
     * @param len the length from start to return
     * @param spec a specification of a comma-separated set of versions
     * @return a JSON document
     */
    public String getTable( short base, int start, int len, String spec )
    {
        if ( this.mvd != null )
            return this.mvd.getTable(base,start,len,spec);
        else
        {
            JSONObject jDoc = new JSONObject();
            JSONArray rows = new JSONArray();
            jDoc.put("rows",rows);
            JSONObject row = new JSONObject();
            rows.add( row );
            JSONArray cells = new JSONArray();
            row.put( "row", cells );
            // first cell
            JSONObject cell1 = new JSONObject();
            cell1.put("class","siglumleft");
            JSONArray segments1 = new JSONArray();
            cell1.put("segments", segments1);
            JSONObject text1 = new JSONObject();
            text1.put("text",version1);
            segments1.add(text1);
            // second cell
            JSONObject cell2 = new JSONObject();
            JSONArray segments2 = new JSONArray();
            cell1.put("segments", segments2);
            JSONObject text2 = new JSONObject();
            text1.put("text",text);
            segments1.add(text2);
            return jDoc.toJSONString();
        }
    }
    public double[][] computeDiffMatrix()
    {
        if ( this.mvd != null )
            return this.mvd.computeDiffMatrix();
        else
        {
            double[][] array = new double[1][];
            array[0] = new double[1];
            return array;
        }
    }
    public String getVersionId( short num )
    {
        if ( this.mvd != null )
            return this.mvd.getVersionId(num);
        else
            return version1;
    }
}
