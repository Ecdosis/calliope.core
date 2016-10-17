/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package calliope.exception;

/**
 *
 * @author desmond
 */
public class AeseException extends Exception {
    /**
     * Create a general MMLException from scratch
     * @param message the message it is to bear
     */
    public AeseException( String message )
    {
        super( message );
    }
    /**
     * Wrapper for another exception
     * @param e the other exception
     */
    public AeseException( Exception e )
    {
        super( e );
    }
}
