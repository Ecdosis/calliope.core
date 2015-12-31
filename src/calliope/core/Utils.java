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

package calliope.core;

import calliope.core.exception.CalliopeException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.BitSet;

/**
 * Some routines that need sharing by all
 * @author desmond
 */
public class Utils 
{
    static HashMap<String,String> map;
    private static final String codes = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    static
    {
        map = new HashMap<String,String>();
        map.put("english","en");
        map.put("italian","it");
        map.put("spanish","es");
    }
    /**
     * Convert a URL into a form suitable as a parameter
     * @param value the raw, unescapedURL
     * @return an escaped URL with / and  space escaped
     */
    public static String escape( String value )
    {
        StringBuilder sb = new StringBuilder();
        {
            for ( int i=0;i<value.length();i++ )
            if ( value.charAt(i) == ' ' )
                sb.append("%20");
            else if ( value.charAt(i) == '/' )
               sb.append("%2F");
            else
                sb.append( value.charAt(i) );
        }
        return sb.toString();
    }
    /**
     * Add a "/"to the end of a path if needed
     * @param path the original path
     * @return the path with ONE trailing slash
     */
    public static String ensureSlash( String path )
    {
        if ( path.endsWith("/") )
            return path;
        else
            return path+"/";
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
    /**
     * Chop off the first component of a urn
     * @param urn the urn to chop
     * @return the first urn component
     */
    public static String first( String urn )
    { 
        int slashPos1 = -1;
        if ( urn.startsWith("/") )
            slashPos1 = urn.indexOf( "/" );
        int slashPos2 = urn.indexOf( "/", slashPos1+1 );
        if ( slashPos1 != -1 && slashPos2 != -1 )
            return urn.substring(slashPos1+1, slashPos2 );
        else if ( slashPos1 != -1 && slashPos2 == -1 )
            return urn.substring( slashPos1+1 );
        else if ( slashPos1 == -1 && slashPos2 != -1 )
            return urn.substring( 0,slashPos2 );
        else
            return urn;
    }
    /**
      * Get the display name for a language given its code+country
      * @param langCode e.g. "it" or "en_GB" etc
      */
     public static String languageName( String langCode )
     {
         String country = langCode;
         if ( langCode.contains("_") )
            country = langCode.substring(0,langCode.indexOf("_"));
        return new Locale(country).getDisplayName();                 
    }
    /**
     * Extract the language code from the docid
     * @param docid the document identifier
     * @return a 2-char language code
     */
    public static String languageFromDocId( String docid )
    {
        String[] parts = docid.split("/");
        if ( map.containsKey(parts[0]) )
            return map.get(parts[0]);
        else
            return "en";
    }
    /**
     * Fetch the last component of a urn
     * @param urn the urn to look at
     * @return the first urn component
     */
    public static String last( String urn )
    {
        int index = urn.lastIndexOf("/");
        if ( index != -1 )
        {
            return urn.substring(index+1);
        }
        else
            return urn;
    }
    /**
     * Extract the second component of a urn
     * @param urn the urn to extract from
     * @return the second urn component
     */
    public static String second( String urn )
    { 
        int start=-1,end=-1;
        for ( int state=0,i=0;i<urn.length();i++ )
        {
            char token = urn.charAt(i);
            switch ( state )
            {
                case 0:// always pass first char
                    state = 1;
                    break;
                case 1: 
                    if ( token == '/' )
                        state = 2;
                    break;
                case 2:
                    start=i;
                    if ( token == '/' )
                    {
                        state = -1;
                        end = i;
                    }
                    else
                        state = 3;
                    break;
                case 3:
                    if ( token == '/' )
                    {
                        end = i;
                        state = -1;
                    }
                    break;
            }
            if ( state == -1 )
                break;
        }
        if ( end == -1 )
            end = urn.length();
        if ( start == -1 )
            start = urn.length();
        return urn.substring( start, end );
    }
    /**
     * Pop off the frontmost part of the path
     * @param path the path to pop
     * @return the popped path
     */
    public static String pop( String path )
    {
        while ( path.length()>0 && path.startsWith("/") )
            path = path.substring(1);
        int pos = path.indexOf("/");
        if ( pos != -1 )
            path = path.substring( pos+1 );
        return path;
    }
    /**
     * Join two paths together with a single slash
     * @param part1 the first path perhaps ending in a slash
     * @param part2 the second path perhaps starting with a slash
     * @return a single-slash joined version of path1 and path2
     */
    public static String canonisePath( String part1, String part2 )
    {
        if ( part1.length()==0 )
            return part2;
        else if ( part1.endsWith("/") )
            if ( part2.startsWith("/") )
                return part1+part2.substring(1);
            else
                return part1+"/"+part2;
        else if ( part2.startsWith("/") )
            return part1+part2;
        else
            return part1+"/"+part2;
    }
    /**
     * Just escape quotes for a string to be jsonified
     * @param input the string to escape
     * @param replaceWith replace double quotes with this
     * @return the escaped string
     */
    public static String escapeQuotes( String input, String replaceWith )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<input.length();i++ )
        {
            char token = input.charAt(i);
            switch ( token )
            {
                case '"':
                    sb.append(replaceWith);
                    break;
                default:
                    sb.append(token);
                    break;
            }
        }
        return sb.toString();
    }
    public static String cleanCR( String value, boolean spaces )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<value.length();i++ )
        {
            if ( value.charAt(i)!='\n'&&value.charAt(i)!='\r' )
            {
                if ( value.charAt(i)=='"' && sb.length()>0 && sb.charAt(sb.length()-1) != '\\')
                    sb.append("\\\"");
                else
                    sb.append(value.charAt(i));
            }
            else if ( spaces )
                sb.append( " " );
        }
        return sb.toString();
    }
    /**
     * Reduce the docid to the language/author/work triple
     * @param a full docid to reduce to its first 3 terms
     * @return a string
     */
    public static String baseDocID( String docid )
    {
        String[] parts = docid.split("/");
        if ( parts.length >= 3 )
            return parts[0]+"/"+parts[1]+"/"+parts[2];
        else
            return docid;
    }
    /**
     * Reduce the docid to language/author
     * @param a full docid to reduce to its first 2 terms
     * @return a string
     */
    public static String shortDocID( String docid )
    {
        String[] parts = docid.split("/");
        if ( parts.length >= 2 )
            return parts[0]+"/"+parts[1];
        else
            return docid;
    }
    /**
     * Separate the group from the full path
     * @param path the path to split
     */
    public static String getGroupName( String path )
    {
        int index = path.lastIndexOf("/");
        if ( index == -1 )
            return "";
        else
            return path.substring( 0, index );
    }
    /**
     * Separate the short name from the full path
     * @param path the path to split
     */
    public static String getShortName( String path )
    {
        int index = path.lastIndexOf("/");
        if ( index == -1 )
            return path;
        else
            return path.substring( index+1 );
    }
    /**
     * Pinched from Tim Bray aerc on GoogleCode
     * @param in the input stream to read from
     * @return a byte array containing the read data
     * @throws IOException 
     */
    public static byte[] readStream(InputStream in) throws IOException 
    {
        byte[] buf = new byte[1024];
        int count = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream(8096);
        while ((count = in.read(buf)) != -1)
            out.write(buf, 0, count);
        return out.toByteArray();
    }
    /**
     * Read a file in as a string
     * @param fname the filename or path
     * @return the file contents as a string, assuming UTF-8
     * @throws CalliopeException 
     */
    public static String readStringFromFile( File input ) 
        throws CalliopeException
    {
        try
        {
            if ( input.exists() )
            {
                FileInputStream fis = new FileInputStream(input);
                byte[] data = new byte[(int)input.length()];
                fis.read(data);
                fis.close();
                return new String(data,"UTF-8");
            }
            else
            {
                throw new FileNotFoundException(input.getName());
            }
        }
        catch ( Exception e )
        {
            throw new CalliopeException( e );
        }
    }
    /**
      * Is the given markup file HTML or something else (e.g. XML)?
      * @param markup
      * @return 
      */
     public static boolean isHtml( String markup )
     {
         StringBuilder sb = new StringBuilder();
         int state = 0;
         for ( int i=0;i<markup.length();i++ )
         {
             char token = markup.charAt(i);
             switch( state )
             {
                 case 0:
                     if ( token == '<' )
                         state = 1;
                     break;
                 case 1:    // seen '<'
                     if ( Character.isLetter(token) )
                     {
                         sb.append( token );
                         state = 2;
                     }
                     else
                         state = 0;
                     break;
                 case 2:    // seen "<[letter]"
                     if ( Character.isWhitespace(token) )
                     {
                         if ( sb.toString().toLowerCase().equals("html") )
                             return true;
                         else
                             return false;
                     }
                     else
                         sb.append(token);
                     break;
             }
         }
         return false;
     }
     /**
      * Encode an array of bytes as a Base64 string
      * @param data
      * @return 
      */
     public static String base64Encode( byte[] in )
     {
        StringBuilder out = new StringBuilder((in.length * 4) / 3);
        int b;
        for (int i = 0; i < in.length; i += 3)  
        {
            b = (in[i] & 0xFC) >> 2;
            out.append(codes.charAt(b));
            b = (in[i] & 0x03) << 4;
            if (i + 1 < in.length)      
            {
                b |= (in[i + 1] & 0xF0) >> 4;
                out.append(codes.charAt(b));
                b = (in[i + 1] & 0x0F) << 2;
                if (i + 2 < in.length)  {
                    b |= (in[i + 2] & 0xC0) >> 6;
                    out.append(codes.charAt(b));
                    b = in[i + 2] & 0x3F;
                    out.append(codes.charAt(b));
                } 
                else  
                {
                    out.append(codes.charAt(b));
                    out.append('=');
                }
            } 
            else      
            {
                out.append(codes.charAt(b));
                out.append("==");
            }
        }
        return out.toString();
     }
     public static byte[] base64Decode(String input)    
     {
        if (input.length() % 4 != 0)    
        {
            throw new IllegalArgumentException("Invalid base64 input");
        }
        byte decoded[] = new byte[((input.length() * 3) / 4) 
            - (input.indexOf('=') > 0 ? (input.length() 
            - input.indexOf('=')) : 0)];
        char[] inChars = input.toCharArray();
        int j = 0;
        int b[] = new int[4];
        for (int i = 0; i < inChars.length; i += 4)     
        {
            // This could be made faster (but more complicated) 
            // by precomputing these index locations
            b[0] = codes.indexOf(inChars[i]);
            b[1] = codes.indexOf(inChars[i + 1]);
            b[2] = codes.indexOf(inChars[i + 2]);
            b[3] = codes.indexOf(inChars[i + 3]);
            decoded[j++] = (byte) ((b[0] << 2) | (b[1] >> 4));
            if (b[2] < 64)      
            {
                decoded[j++] = (byte) ((b[1] << 4) | (b[2] >> 2));
                if (b[3] < 64)  
                {
                    decoded[j++] = (byte) ((b[2] << 6) | b[3]);
                }
            }
        }
        return decoded;
    }
    public static String bitsetToString( BitSet bs )
    {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        int old = 0;
        for ( int v=bs.nextSetBit(0);v>=0;v=bs.nextSetBit(v+1) )
        {
            if ( start==0 )
            {
                sb.append(v);
                old = v;
            }
            else if ( v-start > 1 )
            {
                if ( old<start )
                {
                    sb.append("-");
                    sb.append(start);
                }
                sb.append(",");
                sb.append(v);
                old = v;
            }
            start = v;
        }
        if ( start > old )
        {
            sb.append("-");
            sb.append(start);
        }
        return sb.toString();
    }
//    public static void main(String[]args)
//    {
//        BitSet bs = new BitSet();
//        bs.set(1);
//        bs.set(3);
//        bs.set(4);
//        bs.set(6);
//        bs.set(7);
//        System.out.println(bitsetToString(bs));
//    }
}