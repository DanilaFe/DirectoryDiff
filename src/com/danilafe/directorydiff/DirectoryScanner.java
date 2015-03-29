package com.danilafe.directorydiff;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;

import com.danilafe.directorydiff.DirectoryDiff.Entity;
import com.danilafe.directorydiff.DirectoryDiff.Folder;
import com.danilafe.directorydiff.DirectoryDiff.Item;
import com.sun.jmx.snmp.tasks.Task;

public class DirectoryScanner {

	private JButton[] buttonsL = new JButton[]{
			new JButton("Select First Directory"), new JButton("Select Second Directory")
	};
	private JButton[] copyButtonsL = new JButton[]{
			new JButton("Copy From 1st Folder"), new JButton("Copy From 2nd Folder")
	};
	private JFrame mainFrame = new JFrame();
	private Container top = new Container();
	private Container buttons = new Container();
	private Container copyButtons = new Container();
	private Container center = new Container();
	private DefaultListModel<String> listedDirs = new DefaultListModel<String>();
	private JList list = new JList();
	private JTree tree;
	DefaultMutableTreeNode mainNode;
	private JScrollPane jsp;
	private int dir1 = 0, dir2 = 1;
	final ArrayList<Folder> folders;
	
	public DirectoryScanner(ArrayList<File> files){
		//Index directories
		folders = new ArrayList<Folder>();
		for(File f: files){
			folders.add((Folder)DirectoryDiff.thisS.indexDir(f).setFile(f));
		}
		
		//Populate List
		for(Folder f: folders){
			listedDirs.addElement(f.getName());
			list.setModel(listedDirs);
		}
		
		buttonsL[0].addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dir1 = list.getSelectedIndex();
				updateDiffMap();
			}
		});
		
		buttonsL[1].addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				dir2 = list.getSelectedIndex();
				updateDiffMap();
			}
		});
		
		copyButtonsL[0].addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFrame progressFrame = new JFrame("Progress...");
				final JProgressBar progress = new JProgressBar();
				progress.setMaximum(generateDiffMap(new DefaultMutableTreeNode(), folders.get(dir1), folders.get(dir2)));
				progress.setMinimum(0);
				progress.setValue(0);
				final JLabel status = new JLabel("Current Progress: ");
				
				progressFrame.setSize(400, 150);
				progressFrame.setLayout(new GridLayout(0,1));
				progressFrame.add(status);
				progressFrame.add(progress);
				progressFrame.setVisible(true);
				progressFrame.revalidate();
				
				Thread copyFiles = new Thread() {		
					@Override
					public void run() {
						copyFiles(folders.get(dir1), folders.get(dir2), progress, true, status);
						progressFrame.setVisible(false);
						progressFrame.dispose();
						JOptionPane.showMessageDialog(mainFrame, "Copy Complete!");
						updateDiffMap();
					}
				};
				copyFiles.start();	
			}
		});
		
		copyButtonsL[1].addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFrame progressFrame = new JFrame("Progress...");
				final JProgressBar progress = new JProgressBar();
				progress.setMaximum(generateDiffMap(new DefaultMutableTreeNode(), folders.get(dir2), folders.get(dir1)));
				progress.setMinimum(0);
				progress.setValue(0);
				final JLabel status = new JLabel("Current Progress: ");
				
				progressFrame.setSize(400, 150);
				progressFrame.setLayout(new GridLayout(0,1));
				progressFrame.add(status);
				progressFrame.add(progress);
				progressFrame.setVisible(true);
				progressFrame.revalidate();
				
				Thread copyFiles = new Thread() {		
					@Override
					public void run() {
						copyFiles(folders.get(dir2), folders.get(dir1), progress, true, status);
						progressFrame.setVisible(false);
						progressFrame.dispose();
						JOptionPane.showMessageDialog(mainFrame, "Copy Complete!");
						updateDiffMap();
					}
				};
				copyFiles.start();
			}
		});
		
		top.setLayout(new BorderLayout());
		buttons.setLayout(new GridLayout(0, 2));
		for(JButton b : buttonsL){
			buttons.add(b);
		}
		top.add(new JScrollPane(list), BorderLayout.CENTER);
		top.add(buttons, BorderLayout.NORTH);
		
		mainNode = new DefaultMutableTreeNode("Difference Map");
		tree = new JTree(mainNode);
		jsp = new JScrollPane(tree);
		center.setLayout(new BorderLayout());
		center.add(jsp, BorderLayout.CENTER);
		center.add(new JLabel("Difference Between Selected Directories"), BorderLayout.NORTH);
		copyButtons.setLayout(new GridLayout(0,2));
		for(JButton b : copyButtonsL){
			copyButtons.add(b);
		}
		center.add(copyButtons, BorderLayout.SOUTH);
		
		
		mainFrame.setSize(400, 500);
		mainFrame.setLayout(new BorderLayout());
		mainFrame.add(top, BorderLayout.NORTH);
		mainFrame.add(center, BorderLayout.CENTER);
		mainFrame.setVisible(true);
		
		generateDiffMap(mainNode, folders.get(dir1), folders.get(dir2));
	}
	
	private int generateDiffMap(DefaultMutableTreeNode node, Folder one, Folder two){
		for(Entity e : one.getContainedFiles()){
			if(e instanceof Folder){
				Folder toLook = (Folder) e;
				if(two.getFolder(e.getName()) != null){
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(toLook.getName());
					generateDiffMap(newNode, toLook, two.getFolder(toLook.getName()));
					if(newNode.getChildCount() != 0)
					node.add(newNode);
				} else {
					DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(toLook.getName() + " (Missing Folder)");
					node.add(newNode);
				}
			} else {
				Item toLook = (Item) e;
				if(two.getItem(toLook.getName(), toLook.getExtension()) == null) node.add(new DefaultMutableTreeNode(toLook.getName()));
			}
		}
		int leafCount = node.getLeafCount();
		for(Entity e : two.getContainedFiles()){
			if(!(e instanceof Folder)) {
				Item toLook = (Item) e;
				if(one.getItem(toLook.getName(), toLook.getExtension()) == null) node.add(new DefaultMutableTreeNode(toLook.getName()));
			}
		}
		return leafCount;
	}

	private void copyFiles(Folder one, Folder two, JProgressBar progressBar, boolean addCount, JLabel progress){
		for(Entity e : one.getContainedFiles()){
			if(!(e instanceof Folder)) {
				Item toLook = (Item) e;
				progress.setText("Current Progress: " + "Copying " + toLook.getName());
				if(two.getItem(toLook.getName(), toLook.getExtension()) == null) {
					try {
						File copyFile = toLook.getFile();
						FileInputStream fis = new FileInputStream(copyFile);
						FileChannel in = fis.getChannel();
						File outFile = new File(two.getFile().getAbsolutePath() + File.separator + copyFile.getName());
						if(!outFile.exists()) outFile.createNewFile();
						FileOutputStream fos = new FileOutputStream(outFile);
						FileChannel out = fos.getChannel();
						out.transferFrom( in, 0, in.size() );
						fis.close();
						fos.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
				if(addCount) progressBar.setValue(progressBar.getValue() + 1);
				progressBar.revalidate();
			} else {
				Folder toLook = (Folder) e;
				if(two.getFolder(toLook.getName()) == null) {
					File createFile = new File(two.getFile().getAbsolutePath() + File.separator + toLook.getName());
					createFile.mkdir();
					Folder newFolder = (Folder) new Folder(toLook.getName()).setFile(createFile);
					newFolder.setFile(createFile);
					copyFiles(toLook, newFolder, progressBar, false, progress);
					if(addCount) progressBar.setValue(progressBar.getValue() + 1);
					progressBar.revalidate();
				} else {
					copyFiles(toLook, two.getFolder(toLook.getName()), progressBar, true, progress);
				}
			}
		}
	}
	
	public void updateDiffMap(){
		mainNode.removeAllChildren();
		generateDiffMap(mainNode, folders.get(dir1), folders.get(dir2));
	}
	
}
