/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package calliope.core.handler;

import calliope.core.constants.JSONKeys;
import calliope.core.exception.CalliopeException;
import calliope.core.exception.LoginException;
import org.json.simple.JSONObject;
import calliope.core.login.CookieJar;
import calliope.core.login.Login;
import calliope.core.login.LoginFactory;
import calliope.core.login.LoginType;
import calliope.core.Utils;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Read a link document pointing to a real document
 * @author desmond
 */
public class AeseLink 
{
    /**
     * Add arguments to a raw GET url
     * @param rawURL the base url
     * @param args an array of arguments, each a name, value pair
     * @return the new URL
     */
    private static String appendArgs( String rawURL, ArrayList args )
    {
        StringBuilder sb = new StringBuilder( rawURL );
        for ( int i=0;i<args.size();i++ )
        {
            JSONObject arg = (JSONObject)args.get(i);
            String name = (String)arg.get(JSONKeys.NAME);
            String value = (String)arg.get(JSONKeys.VALUE);
            if ( i == 0 )
                sb.append("?");
            else
                sb.append("&");
            sb.append( name );
            sb.append("=");
            sb.append(value);
        }
        return sb.toString();
    }
    /**
     * Read a general LINK 
     * @param json the link in JSON format
     * @return the data as a byte array. caller should know the format
     */
    private static byte[] readLinkData( JSONObject json ) throws LoginException
    {
        try
        {
            //long start = System.currentTimeMillis();
            String rawURL = (String)json.get(JSONKeys.URL);
            URL url = new URL( rawURL );
            JSONObject login = (JSONObject)json.get(JSONKeys.LOGIN);
            if ( login != null )
            {
                String user = (String)login.get(JSONKeys.USER);
                String password = (String)login.get(JSONKeys.PASSWORD);
                Login  l = null;
                String cookie = null;
                String host = null;
                LoginType lt = LoginType.valueOf((String)login.get(JSONKeys.TYPE));
                l = LoginFactory.createLogin(lt);
                host = url.getHost();
                cookie = CookieJar.cookies.getCookie( user, host );
                if ( cookie == null )
                {
                    cookie = l.login(host, user, password);
                    if ( cookie != null )
                        CookieJar.cookies.setCookie( user, host, cookie );
                }
                //long loginTime = System.currentTimeMillis();
                //System.out.println("login time="+(loginTime-start)+" milliseconds");
                ArrayList args = (ArrayList)json.get( JSONKeys.ARGS );
                if ( args != null )
                    rawURL = appendArgs( rawURL, args );
                URL newUrl = new URL( rawURL );
                HttpURLConnection conn = (HttpURLConnection) newUrl.openConnection();
                conn.setRequestProperty("Cookie", cookie);
                if ( conn.getResponseCode()> 299 )
                {
                    cookie = l.login(host, user, password);
                    if ( cookie != null )
                        CookieJar.cookies.setCookie( user, host, cookie );
                    conn = (HttpURLConnection) newUrl.openConnection();
                    if ( conn.getResponseCode()>299 )
                        throw new Exception( "failed to login "
                            +user+" to "+host);
                }
                InputStream is = conn.getInputStream();
                byte[] bdata = Utils.readStream( is );
                //long end = System.currentTimeMillis();
                //System.out.println("time taken="+(end-start)+" milliseconds");
                return bdata;
            }
            else
                return null;
        }
        catch ( Exception e )
        {
            if ( e instanceof LoginException )
                throw (LoginException)e;
            else
                throw new LoginException( e );
        }
    }
    /**
     * Read a link document
     * @param doc the JSON document representing the link
     * @param cfmt the document content's format
     * @return the document's content in bytes
     * @throws AeseException 
     */
    public static byte[] readLink( JSONObject doc, String cfmt ) 
        throws CalliopeException
    {
        byte[] data = null;
        String url = (String)doc.get( JSONKeys.URL );
        if ( url == null )
            throw new CalliopeException("empty URL");
        try
        {
            data = readLinkData( doc );
            if ( data == null )
                throw new Exception("no data!");
        }
        catch ( Exception e )
        {
            throw new CalliopeException( e );
        }
        return data;
    }
    /**
     * Make a link document out of some parameters
     * @param url 
     * @param user
     * @param password
     * @param lt
     * @param names
     * @param values
     * @return a JSON document being the contents of the link
     */
    public static JSONObject makeLink( String url, String user, String password, 
        LoginType lt, String[] names, String[] values )
    {
        JSONObject link = new JSONObject();
        link.put( JSONKeys.URL, url );
        JSONObject login = new JSONObject();
        login.put( JSONKeys.USER, user );
        login.put( JSONKeys.PASSWORD, password );
        login.put( JSONKeys.TYPE, lt.toString() );
        link.put( JSONKeys.LOGIN, login );
        JSONObject args=new JSONObject();
        ArrayList<JSONObject> list = new ArrayList<JSONObject>();
        for ( int i=0;i<names.length;i++ )
        {
            JSONObject kvPair = new JSONObject();
            kvPair.put( JSONKeys.NAME, names[i] );
            kvPair.put( JSONKeys.VALUE, values[i] );
            list.add( kvPair );
        }
        link.put( JSONKeys.ARGS, list );
        return link;
    }
}