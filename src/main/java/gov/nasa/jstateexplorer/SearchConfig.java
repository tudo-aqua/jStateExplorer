package gov.nasa.jstateexplorer;

import java.io.File;

/**
 *
 * @author mmuesly
 */
public class SearchConfig {
  
  private String resultFolder;
  private boolean enumerativeSearch, symbolicSearch,
          saveTransitionSystem, saveSearchResult;
  private int maxSearchDepth = Integer.MIN_VALUE;
  
  public SearchConfig(){
    setResultFolder("result/default");
    this.enumerativeSearch = false;
    this.symbolicSearch = true;
    this.saveTransitionSystem = false;
    this.saveSearchResult = false;
  }

  public SearchConfig(String resultFolder, boolean enumerative,
          boolean symbolic, int maxDepth, boolean saveTransitionSystem, boolean saveSearchResult){
    setResultFolder(resultFolder);
    this.enumerativeSearch = enumerative;
    this.symbolicSearch = symbolic;
    this.maxSearchDepth = maxDepth;
    this.saveTransitionSystem = saveTransitionSystem;
    this.saveSearchResult = saveSearchResult;
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

  public int getMaxSearchDepth() {
    return maxSearchDepth;
  }

  public void setMaxSearchDepth(int maxSearchDepth) {
    this.maxSearchDepth = maxSearchDepth;
  }

  public boolean isSaveTransitionSystem() {
    return saveTransitionSystem;
  }

  public void setSaveTransitionSystem(boolean saveTransitionSystem) {
    this.saveTransitionSystem = saveTransitionSystem;
  }

  public boolean isSaveSearchResult() {
    return saveSearchResult;
  }

  public void setSaveSearchResult(boolean SaveSearchResult) {
    this.saveSearchResult = SaveSearchResult;
  }
}
