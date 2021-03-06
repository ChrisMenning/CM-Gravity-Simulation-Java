// This is the Main program for Chris Menning's Final project for 21751: Java 4.

// ATTENTION! The more satellites simulated, the more load this will place on your CPU.

// This program simulates gravity, based on Newtonian physics, without the benefit of a physics engine to work with.
// Each PlanetaryBody exerts a gravitational pull on all others, proportional to its mass, and its distance from each other.

// Watch for a few minutes as a randomly generated array of satellites dance around the Earth and moon.
// Keep an eye out for the following rare emergent, unscripted behaviors:
// * Satellites orbiting other satellites.
// * Clustering / Glob / Strand formation

// This project utilizes the following concepts learned in Java 4:
// * Multi-threading - Each PlanetaryBody extends Thread. There are also Threads for sound effects and screen refreshing.
// * Synchronization - Mutex locks help balance all of the concurrent physics calculations across the entire array of objects.
// * I/O - Music and collision sound effect are loaded from resources.
// * Design Patterns - Earth object uses a Singleton design pattern.
// 

// Background Music: "Boy 1904" by J�nsi & Alex, under no valid license, for educational purposes.

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class Main {
	// A list of satellites
	private static List<PlanetaryBody> satellites = new ArrayList<PlanetaryBody>();
	
	// The JFrame where the main scene is rendered, and a JPanel where buttons are shown.
	private static JFrame orbitFrame;
	private static JPanel buttonPanel;
	private static JPanel topPanel;
	
	// A set of boundaries where PlanetaryBodies can be instantiated.
	static int minHoriz;
	static int maxHoriz;
	static int minVert;
	static int maxVert;
	
	// The background music.
	private static File soundFile = new File("src/sounds/Boy_1904.wav");
	private static AudioInputStream audioIn;
	private static Clip clip;

	private static boolean readyAndWillingToPlayMusic;

	private static boolean useSoundFX; 
	
	// The public Getter for the satellites list.
	public static List<PlanetaryBody> getSatellites() {
		return satellites;
	}

	protected static int qtySatellites;

	protected static boolean useEarth;
	protected static boolean useMoon;

	protected static int satLocationCase;

	protected static float initialX;
	protected static float initialY;

	protected static boolean randomizeInitialX;
	protected static boolean randomizeInitialY;

	private static boolean runUpdateLoop;

	protected static boolean useAsteroidsMode;

	private static boolean useCollisons;

	public void setFrame(JFrame thisFrame) {
		setOrbitFrame(thisFrame);
	}
	
	public static void main(String[] args) {
		qtySatellites = 25; 
		randomizeInitialX = false;
		randomizeInitialY = true;
		initialX = -0.5f;
		useEarth = true;
		useMoon = true;
		useCollisons = false;
		useAsteroidsMode = false;
		satLocationCase = 2;
		
		loadBackgroundMusic();
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		makeOptionsFrame();
	}
	
	private static void playMusic() {
		if (!clip.isActive()) {
			clip.setFramePosition(0);
			clip.start();
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		}	
	}
	private static void loadBackgroundMusic() {
		try {
			audioIn = AudioSystem.getAudioInputStream(soundFile);
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
			System.out.println("UNSUPPORTED AUDIO FILE TYPE");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("I/O EXCEPTION while trying to assign 'audioIn'. Wrong path?");
		}
		
		try {
			clip = AudioSystem.getClip();
			clip.open(audioIn);		        
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			System.out.println("FAILED TO GET CLIP");
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("I/O EXCEPTION while trying to get Clip.");
		}
	}

	private static void makeOptionsFrame() {
		
		JFrame optionsFrame = new JFrame("CM Gravity Simulator - Options");
		optionsFrame.setSize(640, 480);
		optionsFrame.setLocationRelativeTo(null);
		optionsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel optionsPanel = new JPanel();
		
		optionsPanel.setLayout(new GridLayout(8,2));
		optionsFrame.add(optionsPanel);
		
		JLabel lblInitialXVel = new JLabel("Initial X Velocity");
		lblInitialXVel.setHorizontalAlignment(SwingConstants.RIGHT);
		optionsPanel.add(lblInitialXVel);
		
		String[] initialVel = {"Random", "-2", "-1.5", "-1", "-0.5", "0", "0.5", "1", "1.5", "2"};
		JComboBox<?> cmbBoxXVel = new JComboBox<String>(initialVel);
		cmbBoxXVel.setSelectedIndex(4);
		cmbBoxXVel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if  (cmbBoxXVel.getSelectedIndex() == 0){
					randomizeInitialX = true;
				} else {
					randomizeInitialX = false;
					initialX = Float.parseFloat(cmbBoxXVel.getSelectedItem().toString());
				}	
			}
		});
		optionsPanel.add(cmbBoxXVel);
		
		JLabel lblInitialYVel = new JLabel("Initial Y Velocity");
		lblInitialYVel.setHorizontalAlignment(SwingConstants.RIGHT);
		optionsPanel.add(lblInitialYVel);
		
		JComboBox<?> cmbBoxYVel = new JComboBox<String>(initialVel);
		cmbBoxYVel.setSelectedIndex(0);
		cmbBoxYVel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (cmbBoxYVel.getSelectedIndex() == 0) {
					randomizeInitialY = true;
				} else {
					randomizeInitialX = false;
					initialY = Float.parseFloat(cmbBoxYVel.getSelectedItem().toString());
				} 
			}
		});
		optionsPanel.add(cmbBoxYVel);
		
		JLabel satellitesQty = new JLabel("Number of satellites: ");
		satellitesQty.setHorizontalAlignment(SwingConstants.RIGHT);
		optionsPanel.add(satellitesQty);
		
		String[] howManySatellites = {"5", "10", "25", "33", "50" };
	 	JComboBox<?> cmbBoxSatellitesQty = new JComboBox<String>(howManySatellites);
	 	cmbBoxSatellitesQty.setToolTipText("How many objects should orbit the Earth?");
	 	cmbBoxSatellitesQty.setSelectedIndex(2);
	 	cmbBoxSatellitesQty.addActionListener(new ActionListener() {
		  public void actionPerformed(ActionEvent e) {
			  qtySatellites = Integer.parseInt(howManySatellites[cmbBoxSatellitesQty.getSelectedIndex()]);
			  
			  System.out.println("You want " + qtySatellites + " satellites.");
		  	}
		});
	 	
	 	satellitesQty.setLabelFor(cmbBoxSatellitesQty);
	 	optionsPanel.add(cmbBoxSatellitesQty);
	 	
	 	JLabel lblSatelliteInstantiationLocation = new JLabel("Where should satellites be created?");
	 	lblSatelliteInstantiationLocation.setHorizontalAlignment(SwingConstants.RIGHT);
	 	optionsPanel.add(lblSatelliteInstantiationLocation);
	 	
	 	String[] satLocs = {"All Around - Extend Beyond Window", 
	 			"All Around - Fit Inside Window", "Above", "Below", "To the Left", "To the Right" };
	 	JComboBox<?> cmbBoxSatLocs = new JComboBox<String>(satLocs);
	 	cmbBoxSatLocs.setToolTipText("Different arrangements of objects have different, interesting effects on the ordiliness of the orbital relationships.");
	 	cmbBoxSatLocs.setSelectedIndex(2);
	 	cmbBoxSatLocs.addActionListener(new ActionListener() {
	 		public void actionPerformed(ActionEvent e) {
	 			satLocationCase = cmbBoxSatLocs.getSelectedIndex();
	 		}
	 	});
	 	optionsPanel.add(cmbBoxSatLocs);

	 	JLabel lblUseEarth = new JLabel("Use Earth?");
	 	lblUseEarth.setHorizontalAlignment(SwingConstants.RIGHT);
	 	optionsPanel.add(lblUseEarth);
	 	
	 	JCheckBox chkBoxUseEarth = new JCheckBox();
	 	chkBoxUseEarth.setToolTipText("What the heck? Who needs it?");
	 	chkBoxUseEarth.setHorizontalAlignment(SwingConstants.LEFT);
	 	chkBoxUseEarth.setSelected(true);
	 	chkBoxUseEarth.addActionListener(new ActionListener() {
	 		public void actionPerformed(ActionEvent e) {
	 			if (chkBoxUseEarth.isSelected())
	 				useEarth = true;
	 			else
	 				useEarth = false;
	 		}
	 	});
	 	optionsPanel.add(chkBoxUseEarth);
	 	
	 	JLabel lblUseMoon = new JLabel("Use Moon?");
	 	lblUseMoon.setHorizontalAlignment(SwingConstants.RIGHT);
	 	optionsPanel.add(lblUseMoon);
	 	
	 	JCheckBox chkBoxUseMoon = new JCheckBox();
	 	chkBoxUseMoon.setToolTipText("It's better with, but sometimes both Earth and the Moon turned of is kinda cool.");
	 	chkBoxUseMoon.setHorizontalAlignment(SwingConstants.LEFT);
	 	chkBoxUseMoon.setSelected(true);
	 	chkBoxUseMoon.addActionListener(new ActionListener() {
	 		public void actionPerformed(ActionEvent e) {
	 			if (chkBoxUseMoon.isSelected())
	 				useMoon = true;
	 			else
	 				useMoon = false;
	 		}
	 	});
	 	optionsPanel.add(chkBoxUseMoon);
	 	
	 	JLabel lblUseAsteroidsMode = new JLabel("'Arcade Mode' (Screen wrap)");
	 	lblUseAsteroidsMode.setHorizontalAlignment(SwingConstants.RIGHT);
	 	optionsPanel.add(lblUseAsteroidsMode);
	 	
	 	JCheckBox chkBoxUseAsteroidsMode = new JCheckBox();
	 	chkBoxUseAsteroidsMode.setToolTipText("Satellite positons loop around to the other side of the screen.");
	 	chkBoxUseAsteroidsMode.setHorizontalAlignment(SwingConstants.LEFT);
	 	chkBoxUseAsteroidsMode.setSelected(false);
	 	chkBoxUseAsteroidsMode.addActionListener(new ActionListener() {
	 		public void actionPerformed(ActionEvent e) {
	 			if (chkBoxUseAsteroidsMode.isSelected())
	 			{
	 				useAsteroidsMode = true;
	 			}
	 			else {
	 				useAsteroidsMode = false;
	 			}
	 		}
	 	});
	 	optionsPanel.add(chkBoxUseAsteroidsMode);
	 	
	 	JButton btnOK = new JButton("OK");
	 	btnOK.addActionListener(new ActionListener() {
	 		public void actionPerformed(ActionEvent e) {
				qtySatellites = Integer.parseInt(howManySatellites[cmbBoxSatellitesQty.getSelectedIndex()]);
	 			createAndDisplayOrbitWindow();
	 		}
	 	});
	 	optionsPanel.add(btnOK, BorderLayout.SOUTH);
	 	
	 	JButton btnExit = new JButton("Exit");
	 	btnExit.addActionListener(new ActionListener() {
	 		public void actionPerformed(ActionEvent e) {
				System.exit(0);
	 		}
	 	});
	 	optionsPanel.add(btnExit);
		optionsFrame.setVisible(true);
		
	}

	protected static void createAndDisplayOrbitWindow() {
		
		//Create a frame
		setOrbitFrame(new JFrame("CM Gravity Simulator"));
		getOrbitFrame().setSize((int)(Toolkit.getDefaultToolkit().getScreenSize().width * 0.75), (int)(Toolkit.getDefaultToolkit().getScreenSize().height * 0.75));
		getOrbitFrame().setDefaultCloseOperation(0);
		getOrbitFrame().setLocationRelativeTo(null);
		getOrbitFrame().setUndecorated(true);
		
		// Make the bottom button panel.
		createButtonPanel();
		
		// Make the top button panel.
		createTopButtonPanel();

		// Finally, set (or re-set) the frame visibility, to ensure all buttons are showing.
		getOrbitFrame().setVisible(true);

		// Instantiate the rest of the satellites
		setSatelliteLocations(satLocationCase);
		instantiateSatellites();
		setSatellitesInMotion();
		
		// Play Sound FX and music
		useSoundFX = true;
		readyAndWillingToPlayMusic = true;
		playMusic();
	}

	private static void createTopButtonPanel() {
		topPanel = new JPanel();
		topPanel.setSize(getOrbitFrame().getWidth(), 35);
		getOrbitFrame().add(topPanel, BorderLayout.NORTH);	

		JButton btnMinWindow = new JButton("Minimize");
		btnMinWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getOrbitFrame().setState(JFrame.ICONIFIED);
			}
		});
		topPanel.add(btnMinWindow);
		
		JButton btnRestoreWindow = new JButton("Restore");
		btnRestoreWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getOrbitFrame().setSize((int)(Toolkit.getDefaultToolkit().getScreenSize().width * 0.75), (int)(Toolkit.getDefaultToolkit().getScreenSize().height * 0.75));;
				getOrbitFrame().setLocationRelativeTo(null);
			}
		});
		topPanel.add(btnRestoreWindow);
		
		JButton btnMaxWindow = new JButton("Maximize");
		btnMaxWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				getOrbitFrame().setExtendedState(getOrbitFrame().getExtendedState() | JFrame.MAXIMIZED_BOTH);
			}
		});
		topPanel.add(btnMaxWindow);
		
		JButton btnCloseWindow = new JButton("Close");
		btnCloseWindow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				killEverythingButTheProgram();
			}
		});
		topPanel.add(btnCloseWindow);
		getOrbitFrame().add(topPanel);
	}

	private static void createButtonPanel() {
		// Create a panel.
		buttonPanel = new JPanel();
		buttonPanel.setBackground(Color.LIGHT_GRAY);
		getOrbitFrame().add(buttonPanel, BorderLayout.SOUTH);
		
		// Create the buttons.
        JButton btnReset = new JButton();
        
        btnReset.setText("Reset");
        btnReset.setToolTipText("Resets positions and initial velocity.");
        btnReset.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            doReset();
          }
        });

        buttonPanel.add(btnReset);
        
        JButton btnGravity = new JButton();
        btnGravity.setText("Toggle Gravity");
        btnGravity.setBackground(Color.CYAN);
        btnGravity.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            doToggleGravity(btnGravity);
          }
        });

        buttonPanel.add(btnGravity);
        
        JButton btnInertia = new JButton();
        btnInertia.setText("Toggle Inertia");
        btnInertia.setBackground(Color.CYAN);
        btnInertia.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
        	  doToggleInertia(btnInertia);
          }
        });

        buttonPanel.add(btnInertia);
        
        JButton btnCollisions = new JButton();
        btnCollisions.setText("Toggle Collisions");
        btnCollisions.setBackground(Color.RED);
        btnCollisions.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		doToggleCollisions(btnCollisions);
        	}
        });
        
        buttonPanel.add(btnCollisions);

        JButton btnMusic = new JButton();
        btnMusic.setText("Toggle Music");
        btnMusic.setBackground(Color.CYAN);
        btnMusic.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		doToggleMusic(btnMusic);
        	}
        });
        
        buttonPanel.add(btnMusic);
        
        JButton btnSound = new JButton();
        btnSound.setText("Toggle Sound FX");
        btnSound.setBackground(Color.CYAN);
        btnSound.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		doToggleSoundFX(btnSound);
        	}
        });
        
        buttonPanel.add(btnSound);
        
        JLabel lbldivisor = new JLabel("Gravity divisor: ");
        lbldivisor.setToolTipText("Lower numbers give stronger gravity. Higher, weaker. 1000 is normal scale."); 
        buttonPanel.add(lbldivisor);
        
        String[] divisors = {"100000", "10000", "5000", "1000", "500", "100" };

		JComboBox<?> cmbBoxGravitydivisor = new JComboBox<String>(divisors);
		cmbBoxGravitydivisor.setSelectedIndex(3);
		cmbBoxGravitydivisor.addActionListener(new ActionListener() {
		  public void actionPerformed(ActionEvent e) {
			  int divisor = Integer.parseInt(divisors[cmbBoxGravitydivisor.getSelectedIndex()]);
		  		for (PlanetaryBody pb : satellites) {
		  			pb.setGravityDivisor(divisor);
		  		}
		  	}
		});
		buttonPanel.add(cmbBoxGravitydivisor);
    }
	
	protected static void doToggleCollisions(JButton btnCollisions) {
		if (useCollisons == true) {
			for (PlanetaryBody pb : satellites) {
				pb.setUseCollisions(false);	
			}
			useCollisons = false;
			btnCollisions.setBackground(Color.RED);
		} else {
			for (PlanetaryBody pb : satellites) {
				pb.setUseCollisions(true);
			}
			useCollisons = true;
			btnCollisions.setBackground(Color.CYAN);
		}
		
	}

	protected static void doToggleSoundFX(JButton btnSound) {
		if (useSoundFX == true) {
			for (PlanetaryBody pb : satellites) {
				// Turn off sound.
				pb.setUseSound(false);
			}
			btnSound.setBackground(Color.RED);
			useSoundFX = false;
		} else {
			for (PlanetaryBody pb : satellites) {
				// Turn off sound.
				pb.setUseSound(true);
			}
			btnSound.setBackground(Color.CYAN);
			useSoundFX = true;
		}
	}

	protected static void doToggleMusic(JButton btnMusic) {
		if (readyAndWillingToPlayMusic == true) {
			System.out.println("Stop music");
			clip.stop();
			clip.close();
			readyAndWillingToPlayMusic = false;
			btnMusic.setBackground(Color.RED);
		} else {
			System.out.println("Start music");
			loadBackgroundMusic();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			playMusic();
			btnMusic.setBackground(Color.CYAN);
			readyAndWillingToPlayMusic = true;
		}
	}

	private static void doReset() {
		killAllSatellites();
		instantiateSatellites();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setSatellitesInMotion();
		
	}
	
	private static void doToggleGravity(JButton btnGravity) {
		for (PlanetaryBody pb : satellites) {
        	if (pb.isUseGravity() == true) {
        		synchronized (pb.lock) {
            		pb.setUseGravity(false);
            		btnGravity.setBackground(Color.RED);
				}
        	}
        	else {
        		synchronized (pb.lock) {
            		pb.setUseGravity(true);
            		btnGravity.setBackground(Color.CYAN);
				}
        	}
        }
	}
	
	private static void doToggleInertia(JButton btnInertia) {
		for (PlanetaryBody pb : satellites) {
        	if (pb.isUseInertia() == true) {
        		synchronized (pb.lock) {
            		pb.setUseInertia(false);
            		btnInertia.setBackground(Color.RED);
				}
        	}
        	else {
        		synchronized (pb.lock) {
            		pb.setUseInertia(true);
            		btnInertia.setBackground(Color.CYAN);
				}
        	}
        }
	}

	private static void instantiateSatellites() {		
		if (useEarth == true) {
			// Create the Earth and add it to the satellites list.
			Earth theEarth = Earth.getInstance();
			theEarth.setX(getOrbitFrame().getWidth()/2);
			theEarth.setY(getOrbitFrame().getHeight()/2);
			satellites.add(theEarth);
		}
		
		if (useMoon == true) {
			//double moonMass = 7.34767309 * (10^22);
			//float moonMass = 205.73f;
			float moonMass = 20;
			System.out.println("Moon's Mass: " + moonMass);
			PlanetaryBody theMoon = new PlanetaryBody("Luna", moonMass, 24, getOrbitFrame().getWidth()/2 - 200, getOrbitFrame().getHeight()/2 +100, randomizeInitialX, randomizeInitialY);
			satellites.add(theMoon);
			
			if (useAsteroidsMode == true) {
				theMoon.setUseAsteroidsMode(true);
			} else {
				theMoon.setUseAsteroidsMode(false);
			}
			qtySatellites--;
		} 
		for (int i = 0; i < qtySatellites; i++)
		{
			int randomXpos = getRandomNumberInRange(minHoriz, maxHoriz);
			int randomYpos = getRandomNumberInRange(minVert, maxVert);
			int randomMass = getRandomNumberInRange(2, 12);
			int pbRadius = randomMass;
			
			PlanetaryBody pb = new PlanetaryBody("Satellite " + i, (long)randomMass, pbRadius, randomXpos, randomYpos, randomizeInitialX, randomizeInitialY);
			satellites.add(pb);
			
			if (useAsteroidsMode == true) {
				pb.setUseAsteroidsMode(true);
			} else {
				pb.setUseAsteroidsMode(false);
			}
			//System.out.println(pb.getPlanetName() + "|" + pb.getMass() +"|" + pb.getRadius() + "|" + randomX + "|" + randomY );
		}
	}
	
	private static void setSatelliteLocations(int satCase) {
		switch(satCase) {
			case 0:
				// All around, extending beyond edges.
				minHoriz = -getOrbitFrame().getWidth()/2;
				maxHoriz = getOrbitFrame().getWidth() + (getOrbitFrame().getWidth()/2);
				minVert = -getOrbitFrame().getHeight()/2;
				maxVert= getOrbitFrame().getHeight() + (getOrbitFrame().getHeight()/2);
				break;
			case 1: // All around, inside edges.
				minHoriz = 0;
				maxHoriz = getOrbitFrame().getWidth();
				minVert = 0;
				maxVert= getOrbitFrame().getHeight();
				break;
			case 2: // Above
				minHoriz = -getOrbitFrame().getWidth()/3;
				maxHoriz = getOrbitFrame().getWidth() + (getOrbitFrame().getWidth()/3);
				minVert = -getOrbitFrame().getHeight() * (2/ 3);
				maxVert= getOrbitFrame().getHeight() /3;
				break;
			case 3: // Below
				minHoriz = -getOrbitFrame().getWidth()/3;
				maxHoriz = getOrbitFrame().getWidth() + (getOrbitFrame().getWidth()/3);
				minVert = getOrbitFrame().getHeight()/2;
				maxVert= getOrbitFrame().getHeight() + getOrbitFrame().getHeight() * (1/2);
				break;
			case 4: // To the left
				minHoriz = -getOrbitFrame().getWidth() * (2/3);
				maxHoriz = (getOrbitFrame().getWidth()/3);
				minVert = -getOrbitFrame().getHeight()/3;
				maxVert= getOrbitFrame().getHeight() + getOrbitFrame().getHeight() /3;
				break;
			case 5:
				// To the Right
				minHoriz = getOrbitFrame().getWidth()/2;
				maxHoriz = getOrbitFrame().getWidth() + (getOrbitFrame().getWidth()/2);
				minVert = -getOrbitFrame().getHeight()/3;
				maxVert= getOrbitFrame().getHeight() + getOrbitFrame().getHeight() /3;
				break;
		}
		
	}

	private static void setSatellitesInMotion() {
		// Set the satellites in motion.
		for (PlanetaryBody p : satellites) {
			if (!p.isAlive()){
				System.out.println(p.getState());
				if (p.getState().toString().equals("NEW"))
					p.start();
			}
		}
		runUpdateLoop = true;
		
		Thread updateThread = new Thread( ) {
			public void run() {
				try {
					System.out.println("Calling updateLoop().");
					updateLoop();
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println("Failed to call updateLoop(). If orbitWindow frame was closed, this is okay.");
				}
				
				System.out.println("updateThread finished.");
			}
		};
		
		if (!updateThread.isAlive())
		{
			System.out.println("UpdateLoop is not alive. Starting it.");
			updateThread.start();
		}
		else {
			System.out.println("UpdateLoop is already alive. Calling Run.");
			updateThread.run();
		}
	}

	// This loop keeps the updateFrame method running.
	private static void updateLoop() {
		System.out.println("Update loop here. I have been called.");
		while(runUpdateLoop == true) {
			updateFrame();
		}
		return;
	}
	
	// Draws one still-frame rendering of the scene.
	public static void updateFrame() {
		int innerBoundaryWidth = getOrbitFrame().getWidth();
		int innerBoundaryHeight = getOrbitFrame().getHeight() - 70;
		Graphics bodyG = getOrbitFrame().getGraphics();

		try {
			Thread.sleep((long)16.67); // Sleep for 1/60th second so the update happens at 60 frames per second.
			Graphics G = getOrbitFrame().getGraphics();
			G.setColor(new Color(6, 16, 23));
			G.fillRect(0, 35, innerBoundaryWidth, innerBoundaryHeight); // Paint background.
			
			for (PlanetaryBody p : satellites) { // Paint all PlanetaryBodies.
				if (p.getX() > 0 && p.getX() < innerBoundaryWidth && p.getY() > 40 && p.getY() < innerBoundaryHeight + 20) {
					bodyG.setColor(p.getColor());
					//draw the ball at the new x and y position
					bodyG.fillOval((int)p.getX() - (int)p.getRadius()/2, (int)p.getY() - (int)p.getRadius()/2, (int)p.getRadius(), (int)p.getRadius());	
				}
			}
		}
		catch(InterruptedException e) {
			System.out.println(e);
		}
	}
	
	public static int getRandomNumberInRange(int min, int max) {
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}

		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}

	private static void killEverythingButTheProgram() {
		if (clip.isActive()) {
			clip.stop();
		}
		killAllSatellites();
		runUpdateLoop = false;
		
		synchronized(satellites) {
			getOrbitFrame().dispose();
		}
		
	}

	private static void killAllSatellites() {
		runUpdateLoop = false;
		for (PlanetaryBody pb : satellites) {
			 {
				
				if (pb.getSatelliteName().equals("Earth")) {
					Earth.reset();
				}
				pb.setKeepAlive(false);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				System.out.println(pb.getSatelliteName() + " state: " + pb.getState());
			}
		}
		satellites.clear();
	}

	public static JFrame getOrbitFrame() {
		return orbitFrame;
	}

	public static void setOrbitFrame(JFrame orbitFrame) {
		Main.orbitFrame = orbitFrame;
	}

}