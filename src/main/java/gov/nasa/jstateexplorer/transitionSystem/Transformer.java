package gov.nasa.jstateexplorer.transitionSystem;

/**
 *
 * @author mmuesly
 */
public interface Transformer<T> {
  public TransitionSystem transformModel(T model);
}
