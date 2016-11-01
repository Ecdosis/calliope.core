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
 *  (c) copyright Desmond Schmidt 2016
 */
package calliope.core;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Determine if a file name is a letter or newspaper etc
 * @author desmond
 */
public class DocType {
    public final static int LETTER = 0;
    public final static int NEWSPAPER = 1;
    public final static int MSORBOOK = 2;
    static final HashMap<String,String> months;
    static final HashSet<String> suffixes;
    static {
        months = new HashMap<String,String>();
        months.put("JAN","January");
        months.put("FEB","February");
        months.put("MAR","March");
        months.put("APR","April");
        months.put("MAY","May");
        months.put("JUN","June");
        months.put("JUL","July");
        months.put("AUG","August");
        months.put("SEP","September");
        months.put("OCT","October");
        months.put("NOV","November");
        months.put("DEC","December");
        suffixes = new HashSet<String>();
        suffixes.add("jpg");
        suffixes.add("gif");
        suffixes.add("png");
        suffixes.add("tif");
        suffixes.add("tiff");
    }
    public static boolean isNumber(String num)
    {
        for ( int i=0;i<num.length();i++ )
        {
            if ( !Character.isDigit(num.charAt(i)) )
                return false;
        }
        return true;
    }
    static String rebuildExcept( String[] parts, int omit )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=parts.length-1;i>=0;i-- )
        {
            if ( i != parts.length-omit )
            {
                if ( sb.length()> 0 )
                    sb.insert(0,"-");
                sb.insert(0,parts[i]);
            }
        }
        return sb.toString();
    }
    public static String isImageFile( String name, int pagePos )
    {
        String suffix = "";
        name = stripSuffix(name);
        if ( suffix.length() ==0 || suffixes.contains(suffix.toLowerCase()) )
        {
            String[] segs = name.split("/");
            if ( segs.length > 0 )
            {
                String[] parts = segs[segs.length-1].split("-");
                if ( parts.length > pagePos ) 
                {
                    if ( DocType.isPage(parts[parts.length-pagePos]) )
                    {
                        return rebuildExcept(parts,pagePos);
                    }
                }
            }
        }
        return null;
    }
    public static boolean isNewspaperImageFile( String name )
    {
        name = isImageFile(name,2);
        return name != null && isNewspaper(name);
    }
    public static boolean isNewspaper( String name )
    {
        boolean res = false;
        String[] segs = name.split("/");
        if ( segs.length > 0 )
        {
            String[] parts = segs[segs.length-1].split("-");
            if ( parts.length > 0 
                && parts[parts.length-1].matches("H[0-9][0-9][0-9][A-Z]+") )
            {
                if ( parts.length > 2 && DocType.isYear(parts[parts.length-2]) )
                    res = true;
                if ( parts.length > 3 && !isMonth(parts[parts.length-3]) )
                    res = false;
                if ( parts.length > 4 && !isDay(parts[parts.length-4]) )
                    res = false;
            }
        }
        return res;
    }
    public static boolean isLetter( String name )
    {
        String[] segs = name.split("/");
        if ( segs.length > 0 )
        {
            String[] parts = segs[segs.length-1].split("-");
            if ( parts.length >= 2 )
            {
                String last = parts[parts.length-1];
                if ( !isName(last)||!isName(parts[parts.length-2]) )
                    return false;
            }
            else
                return false;
            if ( parts.length >= 3 )
            {
                if ( !isYear(parts[parts.length-3]) )
                    return false;
            }
            if ( parts.length >= 4 )
            {
                if ( !isMonth(parts[parts.length-4]) )
                    return false;
            }
            if ( parts.length == 5 )
            {
                if ( !isDay(parts[parts.length-5]) )
                    return false;
            }
            if ( parts.length > 5 )
                return false;
            return true;
        }
        else
            return false;
    }
    public static boolean isLetterImageFile( String name )
    {
        name = isImageFile(name,3);
        return name != null && isLetter(name);
    }
    public static boolean isMSorBookImageFile( String name )
    {
        String suffix = "";
        int i;
        name = stripSuffix(name);
        if ( suffix.length() ==0 || suffixes.contains(suffix.toLowerCase()) )
        {
            for ( i=0;i<name.length();i++ )
            {
                if ( !Character.isDigit(name.charAt(i)) )
                    return false;
                else if ( name.charAt(i)!= '0' )
                    break;
            }
            for ( int j=i;j<name.length();j++ )
            {
                if ( !Character.isDigit(name.charAt(j)) )
                    return false;
            }
            return true;
        }
        else
            return false;
    }
    /**
     * Is this filename component a name?
     * @param name the component to test
     * @return true if it is
     */
    public static boolean isName( String name )
    {
        for ( int i=0;i<name.length();i++ )
        {
            char token = name.charAt(i);
            if ( !Character.isUpperCase(token) && token != '\'' && token != '_' )
                return false;
        }
        return true;
    }
    public static boolean isPage( String page )
    {
        if ( page.startsWith("P") && page.length() > 1 )
        {
            String rest = page.substring(1);
            if ( isNumber(rest) || Utils.isLcRomanNumber(rest) )
                return true;
            else    // mixed: must be Arabic number then letters
            {
                StringBuilder sb = new StringBuilder();
                for ( int i=0;i<rest.length();i++ )
                {
                    if ( Character.isDigit(rest.charAt(i)))
                        sb.append( rest.charAt(i) );
                    else
                        break;
                }
                if ( sb.length()> 0 )
                {
                    rest = rest.substring(sb.length());
                    for ( int i=0;i<rest.length();i++ )
                    {
                        if ( !Character.isLetter(rest.charAt(i)))
                            return false;
                    }
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Is this name-component a year?
     * @param year the year
     * @return true if it is
     */
    public static boolean isYear( String year )
    {
        if ( isNumber(year) )
        {
            int value = Integer.parseInt(year);
            if ( value < 1800 || value > 2016 )
                return false;
            return true;
        }
        else
            return false;
    }
    /**
     * Is this a month-name?
     * @param month the 3-char month name
     * @return 
     */
    public static boolean isMonth( String month )
    {
        return months.containsKey(month);
    }
    /**
     * Is this component a day of the month number?
     * @param day the day-number
     * @return true if it is
     */
    public static boolean isDay( String day )
    {
        if ( isNumber(day) )
        {
            int value = Integer.parseInt(day);
            return value >= 0 && value <= 31;
        }
        else
            return false;
    }
    public static int classify( String docid )
    {
        int doc = DocType.MSORBOOK;
        int index = docid.lastIndexOf("/");
        if ( index != -1 )
        {
            String lastPart = docid.substring(index+1);
            if ( isLetter(lastPart) )
               doc = DocType.LETTER;
            else if ( isNewspaper(lastPart) )
               doc = DocType.NEWSPAPER;
        }
        return doc;
    }
    static String readPageRef( String pageRef )
    {
        if ( pageRef.startsWith("P") )
            return pageRef.substring(1);
        else
            return pageRef;
    }
    /**
     * Get the page number encoded in the file name
     * @param fname the file name
     * @param docType the document type
     * @return the page number
     */
    public static String getPageNo( String fname, int docType )
    {
        fname = stripSuffix(fname);
        String[] parts = fname.split("-");
        switch ( docType )
        {
            case LETTER: 
                return readPageRef(parts[parts.length-3]);
            case NEWSPAPER:
                return readPageRef(parts[parts.length-2]);
            default:
                while ( fname.startsWith("0") )
                    fname = fname.substring(1);
                return fname;
        }
    }
    public static String getMonth( String acronym )
    {
        if ( months.containsKey(acronym) )
            return months.get(acronym);
        else
            return acronym;
    }
    static String stripSuffix( String fname )
    {
        int index = fname.lastIndexOf(".");
        if ( index != -1 )
            fname = fname.substring(0,index);
        return fname;
    }
    /**
     * Does the given docid match the expected type?
     * @param docid the document to look for
     * @param fname the name of the file
     * @param docType the document type
    */
    public static boolean matchFile( String docid, String fname, int docType )
    {
        String[] parts;
        String lastPart = docid;
        fname = stripSuffix(fname);
        int index = docid.lastIndexOf("/");
        if ( index != -1 )
            lastPart = docid.substring(index+1);
        switch ( docType )
        {
            case LETTER:
                parts = fname.split("-");
                fname = rebuildExcept( parts, 3 );
                return fname.equals(lastPart);
            case NEWSPAPER:
                parts = fname.split("-");
                fname = rebuildExcept( parts, 2 );
                return fname.equals(lastPart);
            default:
                return true;
        }
    }
    public static void main(String[] args )
    {
        String newspaper = "10-JUN-1854-H664B";
        String letter = "26-OCT-1863-KENDALL-HARPUR";
        String npImage = "28-JUN-1856-P5A-H330B.jpg";
        String lImage = "07-JUL-1868-P2-KENDALL-PARNELL_MRS.jpg";
        String page = "00000089.jpg";
        String unpage = "1000009A.jpg";
        if ( isNewspaper(newspaper) )
            System.out.println(newspaper+" is a newspaper");
        if ( isLetter(letter) )
            System.out.println(letter+" is a letter");
        if ( isNewspaperImageFile(npImage) )
            System.out.println(npImage+" is a newspaper image file");
        if ( isLetterImageFile(lImage) )
            System.out.println(lImage+" is a letter image file");
        if ( isMSorBookImageFile(page) )
            System.out.println(page+" is a letter MS or book image file");
        if ( !isMSorBookImageFile(unpage) )
            System.out.println(unpage+" is NOT a letter MS or book image file");
        System.out.println("page num in "+npImage+" is "+getPageNo(npImage,NEWSPAPER));
        System.out.println("page num in "+lImage+" is "+getPageNo(lImage,LETTER));
        System.out.println("page num in 00000007.jpg is "+getPageNo("00000007.jpg",MSORBOOK));
    }
}
