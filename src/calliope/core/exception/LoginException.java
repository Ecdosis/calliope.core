/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package calliope.core.exception;

/**
 *
 * @author desmond
 */
public class LoginException extends CalliopeException
{
    public LoginException( Exception e )
    {
        super( e );
    }
    public LoginException( String message )
    {
        super( message );
    }
}
