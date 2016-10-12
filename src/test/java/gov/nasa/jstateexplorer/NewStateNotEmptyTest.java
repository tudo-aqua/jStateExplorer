package gov.nasa.jstateexplorer;

import gov.nasa.jstateexplorer.datastructures.region.EnumerativeRegion;
import gov.nasa.jstateexplorer.datastructures.region.SymbolicRegion;
import gov.nasa.jstateexplorer.datastructures.searchImage.EnumerativeImage;
import gov.nasa.jstateexplorer.datastructures.searchImage.SymbolicImage;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNotNull;

/**
 *
 * @author malte
 */

@Test
public class NewStateNotEmptyTest{ 

  @Test
  public void newStateNotEmpty() {
  EnumerativeImage enumImg = new EnumerativeImage(new EnumerativeRegion());
  assertNotNull(enumImg.getPreviousNewStates());
  assertNotNull(enumImg.getNewStates());

  SymbolicImage symbImg = new SymbolicImage(new SymbolicRegion());
  assertNotNull(symbImg.getPreviousNewStates());
  assertNotNull(symbImg.getNewStates());
  }
}
