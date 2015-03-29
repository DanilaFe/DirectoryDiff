package com.danilafe.directorydiff;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

public class DirectoryDiff {

	static DirectoryDiff thisS;
	private ArrayList<File> files = new ArrayList<File>();
	private JButton[] buttons = new JButton[]{
			new JButton("-"), new JButton("+")
	};
	private JButton scan = new JButton("Scan");
	private JFileChooser choser = new JFileChooser();
	private JFrame frame = new JFrame("Select Directories");
	private DefaultListModel<String> listModel = new DefaultListModel<String>();
	private JList list = new JList();
	
	public DirectoryDiff(){
		init();
	}
	
	public void init(){
		thisS = this;
		//Make sure the list works
		list.setModel(listModel);
		
		//Scan disabled
		scan.setEnabled(false);	
		
		//Scan action listener
		scan.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				scan(files);
			}
		});
		
		//Button action listeners
		buttons[0].addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				manageDir(false);
			}
		});		
		buttons[1].addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				manageDir(true);
			}
		});		
		
		//Container
		Container buttonContainer = new Container();
		buttonContainer.setLayout(new GridLayout(0, buttons.length));	
		for(int i = 0; i < buttons.length; i ++){
			buttonContainer.add(buttons[i]);
		}
				
		//Initiate the frame
		frame.setSize(500, 200);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.add(buttonContainer, BorderLayout.NORTH);
		frame.add(list, BorderLayout.CENTER);
		frame.add(scan, BorderLayout.SOUTH);
		frame.setVisible(true);
	}
	
	protected void scan(ArrayList<File> files) {
		new DirectoryScanner(files);
	}

	public File getDirectory(){
		choser.setDialogTitle("Select a Directory");
		choser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if(choser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
		return choser.getSelectedFile(); 
		return null;
	}
	
	public void manageDir(boolean add) {
		if (add) {
			File selectedFile;
			if ((selectedFile = getDirectory()) != null) {
				// Check if this file has already been added
				boolean exists = false;
				for (File f : files) {
					exists |= f.getAbsolutePath().equals(
							selectedFile.getAbsolutePath());
				}

				if (!exists) {
					files.add(selectedFile);
					listModel.addElement(selectedFile.getAbsolutePath());
				} else {
					JOptionPane.showMessageDialog(frame,
							"This folder has already been added!");
				}
			}
		} else {
			int index = list.getSelectedIndex();
			listModel.remove(index);
			files.remove(index);
		}
		
		if(listModel.size() > 1){
			scan.setEnabled(true);
		} else {
			scan.setEnabled(false);
		}

	}
	
	public Folder indexDir(File directory){
		Folder indexDir =  new Folder(directory.getName());
		if(directory.listFiles() != null)
		for(File f: directory.listFiles()){
			if(f.isDirectory()) indexDir.getContainedFiles().add(indexDir(f).setFile(f));
			else indexDir.getContainedFiles().add(new Item(f.getName(), f.getName().substring(f.getName().lastIndexOf('.') + 1, f.getName().length())).setFile(f));
		}
		
		return indexDir;
	}
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(
			        UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		new DirectoryDiff();
	}
	
	static class Entity {
		
		private String name;
		private File me;
		public Entity(String name){
			this.name = name;
		}
		
		public String getName(){
			return name;
		}
		
		public Entity setFile(File f){
			me = f;
			return this;
		}
		
		public File getFile(){
			return me;
		}
		
	}
	
	static class Folder extends Entity{
		
		private ArrayList<Entity> containedFiles = new ArrayList<Entity>();
		public Folder(String name){
			super(name);
		}
		
		public ArrayList<Entity> getContainedFiles(){
			return containedFiles;
		}
		
		public Folder getFolder(String name){
			for(Entity e : containedFiles){
				if(e instanceof Folder){
					if(((Folder)e).getName().equals(name)) return (Folder) e;
				}
			}
			
			return null;
		}
		
		public Item getItem(String name, String extension){
			for(Entity e : containedFiles){
				if(e instanceof Item){
					if(((Item)e).getName().equals(name) && ((Item)e).getExtension().equals(extension)) return (Item) e;
				}
			}
			
			return null;
		}
		
	}
	
	static class Item extends Entity{
		
		private String extension;
		public Item(String name, String extension){
			super(name);
			this.extension = extension;
		}
		
		public String getExtension(){
			return extension;
		}
	
	}
	
}
