package xmlComparison;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class comparisonTool {
	private JFrame frame;
	static File file1;
	static File file2;
	static Document doc1;
	static Document doc2;
	static int TotalmissingTags;
	static int differentValues;
	static Set<LinkedList<Node>> missingTags = new HashSet<>();
	static Set<LinkedList<Node>> difTextNodes = new HashSet<>();
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					comparisonTool window = new comparisonTool();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public comparisonTool() {
		initialize();
	}

	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 1000, 800);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.NORTH);
		panel.setLayout(new GridLayout(1, 0, 0, 0));
		
		JPanel panel_1 = new JPanel();
		JPanel panel_2 = new JPanel();
		JButton compareBtn = new JButton("Compare Above Files");
		JLabel resultLabel = new JLabel("");
		JButton file1Btn = new JButton("Load File 1");
		JButton file2Btn = new JButton("Load File 2");
		JScrollPane scrollPane = new JScrollPane();
		JScrollPane scrollPane_1 = new JScrollPane();

		JTextPane textPane = new JTextPane();
		JTextPane textPane_1 = new JTextPane();
		
		file1Btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
	            int option = fileChooser.showOpenDialog(frame);
	            if(option == JFileChooser.APPROVE_OPTION){
	               file1 = fileChooser.getSelectedFile();
	               try {
	            	   doc1 = makeDoc(file1);
	            	   textPane.setText(makeStringFromDoc(doc1));
	               } catch (SAXException | IOException e1) {

	            	   e1.printStackTrace();
	               }
	               resultLabel.setText("File Selected: " + file1);
	            }else{
	            	resultLabel.setText("Open command canceled");
	            }
			}
		});
		
		file2Btn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
	            int option = fileChooser.showOpenDialog(frame);
	            if(option == JFileChooser.APPROVE_OPTION){
	               file2 = fileChooser.getSelectedFile();
	               try {
	            	   doc2 = makeDoc(file2);
	            	   textPane_1.setText(makeStringFromDoc(doc2));
	               } catch (SAXException | IOException e1) {
						e1.printStackTrace();
		           }
	               resultLabel.setText("File Selected: " + file2);
	            }else{
	            	resultLabel.setText("Open command canceled");
	            }
			}
		});
		
		compareBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (file1 != null && file2 != null) {
					try {
						missingTags.clear();
						difTextNodes.clear();

						makeComparison(doc1,doc2);
						resultLabel.setText("<html> <font color='red'>number of missing tags: "+ TotalmissingTags +"</font> <font color='blue'>values that are different: "+ differentValues +"</font> </html>");
						
					} catch (SAXException | IOException e1) {
						e1.printStackTrace();
					}
				}
				else {
					resultLabel.setText("requires two files to compare");
				}
			}
		});
		
		panel.add(file1Btn);
		panel.add(file2Btn);
		frame.getContentPane().add(panel_1, BorderLayout.SOUTH);
		panel_1.setLayout(new GridLayout(0, 2, 0, 0));
		panel_1.add(compareBtn);
		panel_1.add(resultLabel);
		frame.getContentPane().add(panel_2, BorderLayout.CENTER);
		panel_2.setLayout(new GridLayout(0, 2, 0, 0));
		panel_2.add(scrollPane);
		panel_2.add(scrollPane_1);

		scrollPane.setViewportView(textPane);
		scrollPane_1.setViewportView(textPane_1);
	}

	static void makeComparison(Document document1,Document document2) throws SAXException, IOException {
			document1.getDocumentElement().normalize();
			document2.getDocumentElement().normalize();
			Element doc1Start = document1.getDocumentElement();
			Element doc2Start = document2.getDocumentElement();
			NodeList child1 = doc1Start.getChildNodes();
			NodeList child2 = doc2Start.getChildNodes();
			
			if(document1.getElementsByTagName("*").getLength() > document2.getElementsByTagName("*").getLength()) {

				missingTags = findMissingTags(child1, child2, document1.getDocumentElement().getNodeName());
				difTextNodes = compareTextNodes(child1,child2, document1.getDocumentElement().getNodeName());
				
			}
			else {
				missingTags = findMissingTags(child2, child1, document2.getDocumentElement().getNodeName());
				difTextNodes = compareTextNodes(child2, child1, document2.getDocumentElement().getNodeName());
			}
			
			TotalmissingTags = missingTags.size();
			differentValues = difTextNodes.size();
			
			// System.out.println(missingTags.size());
			// System.out.println(missingTags);
			// System.out.println(difTextNodes.size());
			// System.out.println(difTextNodes);
	}
	
	static Set<LinkedList<Node>> compareTextNodes(NodeList nl1, NodeList nl2, String root){
		Set<LinkedList<Node>> TextNodeWithDifValues = new HashSet<>();
		
		Boolean tagIsMissing = false;
		
		int j = 0;
		
		for (int i = 0; i < nl1.getLength(); i++) {
			
			if(nl1.item(i).getNodeType() == Node.TEXT_NODE) { 
				if(nl1.item(i).getNodeValue().trim().isEmpty() != true) {
					if(!nl1.item(i).getTextContent().equals(nl2.item(j).getTextContent())) {
						TextNodeWithDifValues.add(getPathToRoot(nl1.item(i), root));
					}
					return TextNodeWithDifValues;
				}
				else {
					if(!tagIsMissing) {
						j++;
					}
				}	
			}
			else {
				if(nl2.item(j) != null && nl1.item(i).getNodeName().equals(nl2.item(j).getNodeName())) {
					
					Set<LinkedList<Node>> temp = compareTextNodes(nl1.item(i).getChildNodes(), nl2.item(j).getChildNodes(), root);
					TextNodeWithDifValues.addAll(temp);
					tagIsMissing = false;
					j++;
				}
				else {
					tagIsMissing = true;
				}
			}
		}
		return TextNodeWithDifValues;
	}
	
	static Set<LinkedList<Node>> findMissingTags(NodeList nl1, NodeList nl2, String root){
		Set<LinkedList<Node>> missingTags = new HashSet<>();
		
		Boolean tagIsMissing = false;
		
		int j = 0;
		
		for (int i = 0; i < nl1.getLength(); i++) {
			if(nl1.item(i).getNodeType() == Node.TEXT_NODE) { 
				if(nl1.item(i).getNodeValue().trim().isEmpty() != true) {
					return missingTags;	
				}
				else {
					if(!tagIsMissing) {
						j++;
					}
				}
			}	
			else {
				if(nl2.item(j) == null || !nl1.item(i).getNodeName().equals(nl2.item(j).getNodeName())) {
					
					missingTags.add(getPathToRoot(nl1.item(i), root));
					tagIsMissing = true;
				}
				else {
					Set<LinkedList<Node>> temp = findMissingTags(nl1.item(i).getChildNodes(), nl2.item(j).getChildNodes(), root);
					missingTags.addAll(temp);
					tagIsMissing = false;
					j++;
				}
			}
		}
			return missingTags;
	}
	
	
	static LinkedList<Node> getPathToRoot(Node n,String root) {
		LinkedList<Node> mylst = new LinkedList<>();
		String temp = "";		
		Node parent = n.getParentNode();
		mylst.add(n);
		mylst.add(parent);
		while(!temp.equals(root)) {
			parent = parent.getParentNode();
			temp=parent.getNodeName();
			mylst.add(parent);
		}
		return mylst;
	}
	
	static Document makeDoc(File file) throws SAXException, IOException {

		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder;
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(file);
			return document;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		return null;

	}
	
	static String makeStringFromDoc(Document doc) {
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans;
		try {
			trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.METHOD, "xml");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(2));

			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(sw);
			DOMSource source = new DOMSource(doc.getDocumentElement());

			trans.transform(source, result);
			String xmlString = sw.toString();
			return xmlString;
		} catch (TransformerException e) {

			e.printStackTrace();
		}
		return null;
	}
}
