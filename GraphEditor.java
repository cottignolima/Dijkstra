package grafo_medio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.TexturePaint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.awt.Rectangle;


class Shape extends Rectangle{

	int id;
	String etichetta;
	static int dim = 50;
	boolean source = false;
	boolean dest = false;
	boolean actual, near;
	boolean visited = false;

	public Shape(int id, int x,int y, String et){
		super (x-dim/2,y-dim/2,dim,dim);
		this.id=id;
		etichetta = et;
	}

	/**
	 * Draw a String centered in the middle of a Rectangle.
	 *
	 * @param g The Graphics instance.
	 * @param text The String to draw.
	 * @param rect The Rectangle to center the text in.
	 */
	public void drawCenteredString(Graphics g, String text, Font font) {
		Rectangle rect = this;
		// Get the FontMetrics
		FontMetrics metrics = g.getFontMetrics(font);
		// Determine the X coordinate for the text
		int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
		// Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
		int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
		// Set the font
		g.setFont(font);
		// Draw the String
		g.drawString(text, x, y);
	}

	void draw(Graphics g){
		g.setColor(Color.WHITE);
		if(source){
			g.setColor(Color.RED);
			g.fillOval(x-10, y-10, dim+20, dim+20);
		}
		if(dest){
			g.setColor(Color.BLUE);
			g.fillOval(x-10, y-10, dim+20, dim+20);
		}
		if(visited)
			g.setColor(Color.LIGHT_GRAY);
		if(actual)
			g.setColor(Color.GREEN);
		if(near)
			g.setColor(Color.YELLOW);

		g.fillOval(x, y, dim, dim);
		g.setColor(Color.BLACK);
		drawCenteredString(g, etichetta, new Font("Times", Font.PLAIN, 23));
	}
}

class Arco implements Serializable{

	Shape from, to;
	float cost;	

	public Arco(Shape from, Shape to, float cost){
		this.from=from;
		this.to=to;
		this.cost=cost;
	}
	
	void draw(Graphics g){
		int fx = (int)from.getCenterX();
		int fy = (int)from.getCenterY();
		int tox = (int)to.getCenterX();
		int toy = (int)to.getCenterY();
		g.drawLine(fx, fy, tox, toy);
		g.drawString(""+cost, (fx+tox)/2, (fy+toy)/2);
	}
}

class ButtonAddEdgeListener implements ActionListener{
	Pannello p;
	JTextField tf;
	public ButtonAddEdgeListener(Pannello p, JTextField tf){
		this.p=p;
		this.tf=tf;
	}
	@Override
	public synchronized void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("+Edge"))
			p.azione = Pannello.Action.ADD_EDGE;
		if(e.getActionCommand().equals("save"))
			p.save();
		if(e.getActionCommand().equals("shortestPath")){
			/*			
			String fromto = JOptionPane.showInputDialog("Inserisci indice dei 2 nodi separati da spazio:");
			String[] parts = fromto.split(" ");

			int from = Integer.parseInt(parts[0]);
			int to = Integer.parseInt(parts[1]);
			 */
			Shape shapeFrom = p.nodoSorgente;
			Shape shapeTo = p.nodoDestinazione;
			Node from = p.punti.get(shapeFrom);
			Node to = p.punti.get(shapeTo);
			p.shortestPath(from,to);
		}
		if(e.getActionCommand().equals("shortestPathStep")){
			Shape shapeFrom = p.nodoSorgente;
			Shape shapeTo = p.nodoDestinazione;
			Node from = p.punti.get(shapeFrom);
			Node to = p.punti.get(shapeTo);
			p.shortestPathStep(from,to);
		}
		if(e.getActionCommand().equals("step")){
			synchronized(p.g){
				p.g.notify();
			}
		}
		if(e.getActionCommand().equals("source"))
			p.azione = Pannello.Action.SELECT_SOURCE;
		if(e.getActionCommand().equals("dest"))
			p.azione = Pannello.Action.SELECT_DEST;

		if(e.getActionCommand().equals("graphviz")){
			p.printGraphViz();
		}

	}

}


class Pannello extends JPanel implements MouseListener, Runnable{

	private static final long serialVersionUID = 5949447457046615521L;
	
	Map<Shape, Node> punti;
	ArrayList<Arco> edges;

	int edgeNumber=0;

	Graph g;

	public static enum Action { SELECT_SOURCE, SELECT_DEST, ADD_NODE, ADD_EDGE};
	Action azione = Action.ADD_NODE;

	transient Shape nodoSorgente, nodoDestinazione;

	Shape edgeA, edgeB;

	public Pannello(){
		punti = new HashMap<>();
		edges = new ArrayList<Arco>();
		g = new Graph(this);

		addMouseListener(this);
		setFocusable(true);
		requestFocus();
		load();
		new Thread(this).start();
	}

	void setDistanceOfNode(Node n, float distanza){
		for (Entry <Shape, Node> riga: punti.entrySet()) {
	        Node nodo = riga.getValue();
	        Shape shp = riga.getKey();
			if(nodo==n){
				shp.etichetta+=","+distanza;
				return;
			}
		}
	}

	void setActual(Node n){
		for (Entry <Shape, Node> riga: punti.entrySet()) {
	        Node nodo = riga.getValue();
	        Shape shp = riga.getKey();
			shp.actual=false;
			shp.near=false;//ogni volta che imposto il nodo attuale, resetto tutti quelli che avevo segnato come vicini al ciclo prima
			if(nodo==n)
				shp.actual=true;
		}
	}

	void setNear(Node n){
		for (Entry <Shape, Node> riga: punti.entrySet()) {
	        Node nodo = riga.getValue();
	        Shape shp = riga.getKey();
			shp.near=false;
			if(nodo==n){
				shp.near=true;
			}
		}
	}

	void setVisited(Node n){
		for (Entry <Shape, Node> riga: punti.entrySet()) {
	        Node nodo = riga.getValue();
	        Shape shp = riga.getKey();
			if(nodo==n){
				shp.visited=true;
				return;
			}
		}
	}

	public boolean load() {
		Pannello e = null;
		try {
			FileInputStream fileIn = new FileInputStream("employee.ser");
			ObjectInputStream in = new ObjectInputStream(fileIn);
			e = (Pannello) in.readObject();
			punti = e.punti;
			edges = e.edges;
			g = e.g;

			in.close();
			fileIn.close();
		} catch (FileNotFoundException i) {
			System.out.println("load non ha trovato il file");
			return false;
		} catch (IOException i) {
			i.printStackTrace();
			return false;
		} catch (ClassNotFoundException c) {
			System.out.println("Employee class not found");
			c.printStackTrace();
			return false;
		}
		return true;
	}

	public void save() {
		Pannello e = this;

		try {
			FileOutputStream fileOut =
					new FileOutputStream("employee.ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(e);
			out.close();
			fileOut.close();
			System.out.printf("Serialized data is saved in employee.ser\n");
		} catch (IOException i) {
			i.printStackTrace();
		}
	}

	public void saveTXT() {
		try {
			File file = new File("map.txt");
			file.createNewFile();
			FileWriter writer = new FileWriter(file); 

			writer.write(punti.size()+"\n"); 
			for(Shape r : punti.keySet()){
				writer.write(r.id+" "+r.getCenterX()+" "+r.getCenterY()+" "+r.etichetta+"\n"); 
			}
			writer.write(edges.size()+"\n"); 
			for(Arco e : edges){
				writer.write(e.from+" "+e.to+" "+e.cost+"\n"); 
			}

			writer.flush();
			writer.close();

			System.out.printf("Serialized data is saved in map.txt");
		} catch (IOException i) {
			i.printStackTrace();
		}
	}
	
	public void printGraphViz(){
		/*
	 	http://www.webgraphviz.com/
		 
		graph (undirected) 
		digraph (directed) 
		"->" (arco diretto) 
 		"--" (arco senza direzione)
 		ankdir=LR; (direzione in cui va il grafo Left->Right), senza specifiche va in verticale
 		
		graph nome_grafo {
			node [shape = circle];
			LR_0 -- LR_2 [ label = "SS(B)" ];
			LR_0 -- LR_1 [ label = "SS(S)" ];
		}
		*/		
		System.out.println("graph nome_grafo {");
		System.out.println("\tnode [shape = circle];");
		System.out.println("\trankdir=LR;");//direzione in cui va il grafo Left->Right
		for(Arco e : edges)			
			System.out.println("\t"+punti.get(e.from).toString()+" -- "+punti.get(e.to).toString()+" [ label = \""+e.cost+"\" ];");
		System.out.println("}");
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.drawString("N:"+punti.size(), 20, 20);
		g.drawString("E:"+edges.size(), 20, 40);
		for(Arco s : edges) 
			s.draw(g);
		for(Shape s : punti.keySet()) 
			s.draw(g);

	}


	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		if(azione==Action.ADD_NODE){

			Node n = new Node("N_"+punti.size());
			g.addNode(n);

			punti.put(new Shape(punti.size(),e.getX(),e.getY(),"N_"+punti.size()), n);
			repaint();
			return;
		}

		for(Shape s : punti.keySet()){
			if(s.contains(new Point(e.getX(),e.getY()))){

				if(azione==Action.ADD_EDGE){
					if(edgeNumber==0)
						edgeA = s;
					else{
						if(edgeA==s) return;
						edgeB=s;
						String path = JOptionPane.showInputDialog("Inserisci costo arco:");
						float costo = Float.parseFloat(path);

						Node nodeA = punti.get(edgeA);
						Node nodeB = punti.get(edgeB);
						g.addUndirectedEdge(nodeA, nodeB, costo);

						edges.add(new Arco(edgeA,edgeB,costo));
						repaint();
					}
					edgeNumber = (edgeNumber+1)%2;
				}

				if(azione==Action.SELECT_SOURCE){
					s.source = false;
					nodoSorgente = s;
					s.source = true;
					repaint();
				}
				if(azione==Action.SELECT_DEST){
					s.dest = false;
					nodoDestinazione = s;
					s.dest = true;
					repaint();
				}			

			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public void shortestPath(Node source, Node dest){
		g.shortestPathAnim(source, dest);
	}
	public void shortestPathStep(Node source, Node dest){
		g.shortestPathStep(source, dest);
	}

	@Override
	public void run() {
		while(true){
			repaint();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

public class GraphEditor {

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//     	frame.setSize(2024, 2024);
		frame.setSize(1300, 600);
		frame.setLayout(null);


		Pannello p = new Pannello();
		p.setBounds(0,0,1000,600);
		frame.add(p);

		p.requestFocus();
		p.setFocusable(true);

		JTextField tf = new JTextField("");
		tf.setBounds(1000, 0, 300, 40);
		frame.add(tf);

		JButton b = new JButton("+Edge");
		b.setActionCommand("+Edge");
		b.setBounds(1000, 40, 300, 40);
		frame.add(b);

		ButtonAddEdgeListener l = new ButtonAddEdgeListener(p,tf);
		b.addActionListener(l);

		JButton b2 = new JButton("save");
		b2.setBounds(1000, 80, 300, 40);
		frame.add(b2);
		b2.setActionCommand("save");
		b2.addActionListener(l);

		JButton b3 = new JButton("percorso animato");
		b3.setBounds(1000, 120, 300, 40);
		frame.add(b3);
		b3.setActionCommand("shortestPath");
		b3.addActionListener(l);

		JButton b4 = new JButton("select source");
		b4.setBounds(1000, 160, 300, 40);
		frame.add(b4);
		b4.setActionCommand("source");
		b4.addActionListener(l);

		JButton b5 = new JButton("select dest");
		b5.setBounds(1000, 200, 300, 40);
		frame.add(b5);
		b5.setActionCommand("dest");
		b5.addActionListener(l);
		
		JButton b6 = new JButton("start passo passo");
		b6.setBounds(1000, 240, 300, 40);
		frame.add(b6);
		b6.setActionCommand("shortestPathStep");
		b6.addActionListener(l);

		JButton b7 = new JButton("passo");
		b7.setBounds(1000, 280, 300, 40);
		frame.add(b7);
		b7.setActionCommand("step");
		b7.addActionListener(l);

		JButton b8 = new JButton("graphviz");
		b8.setBounds(1000, 320, 300, 40);
		frame.add(b8);
		b8.setActionCommand("graphviz");
		b8.addActionListener(l);
		
		frame.setVisible(true);
	}

}
