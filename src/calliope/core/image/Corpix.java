/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package calliope.core.image;

import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.exception.ImageException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import calliope.core.constants.Database;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;

/**
 * Support image get/put etc using a simple file system stored at 
 * web-root (/var/www/corpix/)
 * @author desmond
 */
public class Corpix {
    private static final String webRoot = "/var/www/";
    /**
     * List all the images
     * @param docID the docID including the versionID
     * @return an array of docIDs
     * @throws ImageException 
     */
    public static String[] listImages( String docID ) throws ImageException
    {
        try
        {
            ArrayList<String> files = new ArrayList<String>();
            File dir = new File( webRoot+"corpix/"+docID );
            File[] contents = dir.listFiles();
            for ( int i=0;i<contents.length;i++ )
            {
                String name = contents[i].getName();
                if ( name.equals("..")||name.equals(".") )
                    continue;
                else 
                    files.add( docID+"/"+name );
            } 
            String[] arr = new String[files.size()];
            files.toArray( arr );
            return arr;
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
    /**
     * Given a file name derive its mimetype
     * @param f the file
     * @return the mimetype as a string
     */
    private static String mimeTypeFromFile( File f ) throws ImageException
    {
        try
        {
            Path p = f.toPath();
            String name;
            if ( Files.isSymbolicLink(p) )
            {
                Path target = Files.readSymbolicLink(p);
                name = target.getFileName().toString();
            }
            else
                name = f.getName();
            return MimeType.getContentType( name );
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
    /**
     * Fetch a particular image
     * @param docID the image's docID
     * @param mt return the image's mimetype
     * @return the image data as a byte array
     * @throws ImageException 
     */
    public static byte[] getImage( String docID, MimeType mt ) throws ImageException
    {
        try
        {
            File f = new File( webRoot+"corpix/"+docID );
            if ( f.exists() )
            {
                FileInputStream fis = new FileInputStream( f );
                byte[] data = new byte[(int)f.length()];
                mt.mimeType = mimeTypeFromFile(f);
                fis.read( data );
                fis.close();
                return data;
            }
            else
                throw new FileNotFoundException(docID);
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
    /**
     * Delete the image matching the docID
     * @param docID the image docID
     * @throws ImageException 
     */
    public static void deleteImage( String docID ) throws ImageException
    {
        try
        {
            File f = new File(webRoot+"corpix/"+docID);
            if ( f.exists() )
                f.delete();
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
    /**
     * Split a docid into its final component and the rest
     * @param docID the docid
     * @return an array of two strings
     */
    private static String[] splitDocID( String docID )
    {
        String[] parts = new String[2];
        int pos = docID.lastIndexOf("/");
        if ( pos != -1 )
        {
            parts[0] = docID.substring(0,pos);
            parts[1] = docID.substring(pos+1);
        }
        else
        {
            parts[0] = docID;
            parts[1] = "";  
        }
        return parts;
    }
    /**
     * Write an uploaded image to the image store
     * @param docID the mage docID
     * @param mimeType its type
     * @param data its data
     * @throws ImageException 
     */
    public static void addImage( String docID, String mimeType, byte[] data ) 
        throws ImageException
    {
        try
        {
            String[] parts = splitDocID( docID );
            String suffix = MimeType.getFileSuffix( mimeType );
            File dir = new File( webRoot+"corpix/"+parts[0]);
            if ( !dir.exists() )
                dir.mkdirs();
            File image = new File( dir, parts[1]+suffix );
            if ( image.exists() )
                image.delete();
            FileOutputStream fos = new FileOutputStream( image );
            fos.write( data );
            fos.close();
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
    /**
     * Rename an image
     * @param oldDocID the old docID
     * @param newDocID the new docID
     * @param mimeType the new mime type
     * @throws ImageException if something went wrong
     */
    public static void renameImage( String oldDocID, String newDocID, 
        String mimeType ) throws ImageException
    {
        try
        {
            File old = new File( webRoot+"corpix/"+oldDocID );
            if ( old.exists() )
            {
                String[] parts = splitDocID( newDocID );
                String suffix = MimeType.getFileSuffix( mimeType );
                String newFileName = webRoot+"corpix/"+parts[0]+suffix;
                old.renameTo( new File(old.getParentFile(),newFileName) );
            }
            else
                throw new FileNotFoundException( "file not found "+oldDocID);
        }
        catch ( Exception e )
        {
            throw new ImageException(e);
        }
    }
    /**
     * Get the metadata associated with the file from the DB
     * @param docID the document ID
     * @return the metadata as a JSON document
     * @throws ImageException 
     */
    public static String getMetaData( String docID ) throws ImageException
    {
        try
        {
            Connection conn = Connector.getConnection();
            String md = conn.getFromDb( Database.METADATA, "corpix/"+docID);
            if ( md != null )
            {
                return md;
            }
            else
            {
                // no actual metadata: create and store it for next time
                MimeType mt = new MimeType("");
                byte[] data = getImage(docID,mt);
                if ( data != null )
                {
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    BufferedImage bimg = ImageIO.read(bis);
                    int width = bimg.getWidth();
                    int height = bimg.getHeight();
                    ImgInfo ii = new ImgInfo( width, height, mt.mimeType);
                    String jDoc = ii.toString();
                    conn.putToDb( Database.METADATA, "corpix/"+docID, jDoc );
                    return jDoc;
                }
                else 
                    throw new Exception("Image not found "+docID);
            }
        }
        catch ( Exception e )
        {
            throw new ImageException( e );
        }
    }
}
