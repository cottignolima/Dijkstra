package grafo_medio;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class Property implements Serializable{
	boolean visited;
	float distance;
	Node prevNode;

	public Property(){
		this.visited=false;
		this.distance=999999;
		this.prevNode=null;
	}

	public Property(boolean visited, float distance, Node prevNode){
		this.visited=visited;
		this.distance=distance;
		this.prevNode=prevNode;
	}
}

public class Graph implements Serializable, Runnable{

	Pannello p;
	Node source, dest;
	final long animDelay = 2000;
	boolean stepByStep = false;

	private Set<Node> nodes = new HashSet<>();

	public Graph(Pannello p){
		this.p=p;
	}	 

	public void addNode(Node nodeA) {
		nodes.add(nodeA);
	}

	public void addUndirectedEdge(Node nodeA, Node nodeB, float costo){
		nodeA.addDestination(nodeB, costo);
		nodeB.addDestination(nodeA, costo);
	}

	public void shortestPathAnim(Node source, Node dest) {
		this.source=source;
		this.dest=dest;
		new Thread(this).start();
	}

	public void shortestPathStep(Node source, Node dest) {
		this.source=source;
		this.dest=dest;
		stepByStep = true;
		new Thread(this).start();
	}

	public synchronized List<Node> shortestPath(Node source, Node dest) {

		Set<Node> listaDiAttesa = new HashSet<>();
		Map<Node, Property> nodeProp = new HashMap<Node, Property>();
		for(Node n : nodes){
			nodeProp.put(n, new Property());
			listaDiAttesa.add(n);   	
		}

		nodeProp.get(source).distance = 0;

		while (listaDiAttesa.size() != 0) {

			System.out.println("In attesa di essere visitati: "+listaDiAttesa.toString());
			Node currentNode = getNodoPiuVicino(listaDiAttesa, nodeProp);
			listaDiAttesa.remove(currentNode);
			nodeProp.get(currentNode).visited = true;
			System.out.println("Scelgo il più vicino: "+currentNode.toString());

			//per grafica
			p.setActual(currentNode);
			p.setVisited(currentNode);
			p.setDistanceOfNode(currentNode, nodeProp.get(currentNode).distance);	
			try {
				if(stepByStep) {
					wait();
				}else
					Thread.sleep(animDelay);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (Entry <Node, Float> adjacencyPair: currentNode.getNodiAdiacenti().entrySet()) {
				Node nodoVicino = adjacencyPair.getKey();
				Float costoCammino = adjacencyPair.getValue();

				if(!nodeProp.get(nodoVicino).visited){				
					System.out.print("Il nodo vicino "+nodoVicino.toString()+ " ha distanza dal punto di partenza "+nodeProp.get(nodoVicino).distance);
					//per grafica
					p.setNear(nodoVicino);    
					try {
						if(stepByStep) 
							wait();
						else
							Thread.sleep(animDelay);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(nodeProp.get(nodoVicino).distance>nodeProp.get(currentNode).distance+costoCammino){
						nodeProp.get(nodoVicino).distance=nodeProp.get(currentNode).distance+costoCammino;
						nodeProp.get(nodoVicino).prevNode=currentNode;		
						System.out.print(" e la sostituisco con "+nodeProp.get(nodoVicino).distance);
						//per grafica
						p.setDistanceOfNode(nodoVicino, nodeProp.get(nodoVicino).distance);
						try {
							if(stepByStep) 
								wait();
							else
								Thread.sleep(animDelay);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}


					}
				}
				System.out.println();              
			}
		}

		LinkedList<Node> lout = new LinkedList<Node> ();
		Node u=dest;
		String s="";
		float tot=nodeProp.get(u).distance;

		while (nodeProp.get(u)!=null) {
			lout.addFirst(u);
			s = u.toString()+","+s;
			u=nodeProp.get(u).prevNode;
		}
		System.out.println("Finito! il percorso migliore è: ["+s.substring(0, s.length()-1)+"] di lunghezza "+tot);

		return lout;

	}

	private Node getNodoPiuVicino(Set <Node> nodi, Map<Node, Property> nodeProp) {
		Node lowestDistanceNode = null;
		float lowestDistance = Float.MAX_VALUE;
		for (Node node: nodi) {
			float nodeDistance = nodeProp.get(node).distance;
			if (nodeDistance < lowestDistance) {
				lowestDistance = nodeDistance;
				lowestDistanceNode = node;
			}
		}
		return lowestDistanceNode;
	}

	@Override
	public void run() {
		shortestPath(source,dest);
	}

}

