package grafo_medio;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Node implements Serializable{
    
    private String name;         
    Map<Node, Float> nodiAdiacenti = new HashMap<>();
 
    public void addDestination(Node destination, float distance) {
    	nodiAdiacenti.put(destination, distance);
    }
  
    public Node(String name) {
        this.name = name;
    }
     
    public Float getDistanzaDalNodo(Node n){
    	return nodiAdiacenti.get(n);
    }
    
    public Map<Node, Float> getNodiAdiacenti(){
    	return nodiAdiacenti;
    }
    
    public String toString(){
    	return name;
    }
}