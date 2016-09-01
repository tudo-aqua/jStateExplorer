package gov.nasa.jstateexplorer;

import java.io.File;

/**
 *
 * @author mmuesly
 */
public class SearchConfig {
  
  private String resultFolder;
  private boolean enumerativeSearch, symbolicSearch, synchronisedSearch;
  private int maxSearchDepth = Integer.MIN_VALUE;
  
  public SearchConfig(){
    setResultFolder("result/default");
    enumerativeSearch = false;
    symbolicSearch = true;
    synchronisedSearch = false;
  }

  public SearchConfig(String resultFolder, boolean enumerative,
          boolean symbolic, boolean synchronised, int maxDepth){
    setResultFolder(resultFolder);
    enumerativeSearch = enumerative;
    symbolicSearch = symbolic;
    synchronisedSearch = synchronised;
    maxSearchDepth = maxDepth;
  }
  public String getResultFolder() {
    return resultFolder;
  }

  public void setResultFolder(String resultFolder) {
    File file = new File(resultFolder);
    if(!file.exists()){
      file.mkdirs();
    }
    this.resultFolder = resultFolder;
  }

  public boolean shouldUseEnumerativeSearch() {
    return enumerativeSearch;
  }

  public void setEnumerativeSearch(boolean enumerativeSearch) {
    this.enumerativeSearch = enumerativeSearch;
  }

  public boolean shouldUseSymbolicSearch() {
    return symbolicSearch;
  }

  public void setSymbolicSearch(boolean symbolicSearch) {
    this.symbolicSearch = symbolicSearch;
  }

  public boolean shouldUseSynchronisedSearch() {
    return synchronisedSearch;
  }

  public void setSynchronisedSearch(boolean synchronisedSearch) {
    this.synchronisedSearch = synchronisedSearch;
  }

  public int getMaxSearchDepth() {
    return maxSearchDepth;
  }

  public void setMaxSearchDepth(int maxSearchDepth) {
    this.maxSearchDepth = maxSearchDepth;
  }
  
  
}
