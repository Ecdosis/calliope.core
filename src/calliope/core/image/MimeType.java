package calliope.core.image;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.HashMap;

/**
 * Store the mime type of an image
 * @author desmond
 */
public class MimeType 
{
    public String mimeType;
    static HashMap<String,String> map;
    static HashMap<String,String> reverse;
    static 
    {
        map = new HashMap<String,String>();
        map.put( ".jpg", "image/jpeg");
        map.put(".png", "image/png");
        map.put(".bmp","image/bmp");
        map.put(".gif","image/gif");
        map.put(".tif","image/tiff");
        reverse = new HashMap<String,String>();
        reverse.put( "image/jpeg",".jpg");
        reverse.put("image/png",".png");
        reverse.put("image/bmp",".bmp");
        reverse.put("image/gif",".gif");
        reverse.put("image/tiff",".tif");
        
    }
    public MimeType( String type )
    {
        this.mimeType = type;
    }
    /**
     * Empty constructor to fill in later
     */
    public MimeType()
    {
    }
    public static String getFileSuffix( String mimeType )
    {
        if ( reverse.containsKey(mimeType) )
            return reverse.get(mimeType);
        else
            return "";
    }
    public static String getContentType( String fileName )
    {
        int pos = fileName.lastIndexOf(".");
        if ( pos != -1 )
        {
            String suffix = fileName.substring(pos);
            if ( map.containsKey(suffix) )
                return map.get(suffix);
        }
        return "application/octet-stream";
    }
}
