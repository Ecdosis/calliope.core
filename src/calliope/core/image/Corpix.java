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
package calliope.core.image;

import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.exception.ImageException;
import calliope.core.Utils;
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
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Support image get/put etc using a simple file system stored at 
 * web-root (/var/www/corpix/)
 * @author desmond
 */
public class Corpix {
    /**
     * List all the images
     * @param webRoot the root directory of the web server
     * @param docID the docID including the versionID
     * @return an array of docIDs
     * @throws ImageException 
     */
    public static String[] listImages( String webRoot, String docID ) 
        throws ImageException
    {
        try
        {
            ArrayList<String> files = new ArrayList<String>();
            File dir = new File( webRoot+"/corpix/"+docID );
            File[] contents = null;
            while ( contents == null && docID.length()>0 )
            {
                contents = dir.listFiles();
                if ( contents == null )
                {
                    docID = Utils.chomp(docID);
                    dir = new File( webRoot+"corpix/"+docID );
                }
            }
            if ( contents == null )
                throw new Exception("No images in path "
                    +dir.getAbsolutePath());
            else
            {
                for ( int i=0;i<contents.length;i++ )
                {
                    String name = contents[i].getName();
                    if ( name.equals("..")||name.equals(".") )
                        continue;
                    else if ( contents[i].isFile() 
                        && name.indexOf(".png")==-1 
                        && name.indexOf(".jpg")==-1 )
                        files.add( docID+"/"+name );
                } 
                String[] arr = new String[files.size()];
                files.toArray( arr );
                return arr;
            }
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
    public static byte[] getImage( String webRoot, String docID, MimeType mt ) 
        throws ImageException
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
    public static void deleteImage( String webRoot, String docID ) 
        throws ImageException
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
     * @param docID the image docID
     * @param fileName the original file name
     * @param link the link name or null
     * @param mimeType its type
     * @param data its data
     * @throws ImageException 
     */
    public static void addImage( String webRoot, String docID, String fileName,
        String link, String mimeType, byte[] data ) throws ImageException
    {
        try
        {
            String[] parts = splitDocID( docID );
            StringBuilder sb = new StringBuilder( webRoot );
            sb.append( "/corpix" );
            for ( int i=0;i<parts.length-1;i++ )
            {
                sb.append("/");
                sb.append(parts[i]);
            }
            System.out.println("image path="+sb);
            File dir = new File( sb.toString() );
            if ( !dir.exists() )
                dir.mkdirs();
            System.out.println("Created image path");
            File image = new File( dir, fileName );
            if ( image.exists() )
                image.delete();
            System.out.println("Image file cleared");
            image.createNewFile();
            FileOutputStream fos = new FileOutputStream( image );
            fos.write( data );
            fos.close();
            // create icon
            if ( link != null && link.length() > 0 )
            {
                Path newLink = Paths.get(sb.toString()+"/"+link);
                Path target = Paths.get(sb.toString()+"/"+fileName);
                try {
                    Files.createSymbolicLink(newLink, target);
                } catch (IOException x) {
                    System.err.println(x);
                } catch (UnsupportedOperationException x) {
                    // Some file systems do not support symbolic links.
                    System.err.println(x);
                }

            }
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
    public static void renameImage( String webRoot, String oldDocID, String newDocID, 
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
    public static String getMetaData( String webRoot, String docID ) 
        throws ImageException
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
                byte[] data = getImage(webRoot,docID,mt);
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
