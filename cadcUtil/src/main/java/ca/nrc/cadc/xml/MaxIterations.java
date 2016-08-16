
package ca.nrc.cadc.xml;

/**
 * Interface to limit the IterableContent.
 * 
 * @author pdowler
 */
public interface MaxIterations
{
    
    long getMaxIterations();
    
    void maxIterationsReached();

}
