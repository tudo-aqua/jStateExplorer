package gov.nasa.jstateexplorer.newTransitionSystem.helper;

/**
 *
 * @author mmuesly
 */
public class TransitionIDGenerator {
  private static long ID = 0;
  
  public  static synchronized long getNext() {
    ID++;
    return ID;
  }
  
}
