package gov.nasa.jstateexplorer.transitionSystem;

/**
 *
 * @author malte
 */


public class TransitionMonitor {
    private static boolean runing = true;
    private TransitionMonitor(){
        
    }
    
    public static boolean isRuning(){
        return runing;
    };
    
    public static void stopRuning(){
        runing = false;
    }
    
    public static void startRuning(){
        runing = true;
    }
}

