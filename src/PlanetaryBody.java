// This is the base PlanetaryBody class.

// Gravity physics based heavily on Newton's theorem F = g(m1*m2)/d^2.
// Inertia physics created through my own guess-and-test work on this project.

// Collision detection and bounce reaction based on util-elastic-collision.js by Christopher Lis, found here: 
// https://gist.github.com/christopher4lis/f9ccb589ee8ecf751481f05a8e59b1dc

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class PlanetaryBody extends Thread implements GravitationalConstants{
	
	/*
	 * The Private Fields
	 * ==================
	 */
	
	// The planet's name and physical properties (fields).
	private String satelliteName;
	private float mass; 
	private int radius;
	private double xPos;
	private double yPos;
	private double velocityX;
	private double velocityY;
	private Rectangle collider;
	private Color color;
	
	// Behavior modifiers
	private boolean useGravity;
	private boolean useInertia;
	private boolean useCollisions;
	private boolean useSound;
	private boolean keepAlive;
	private double gravitydivisor = 1000;
	private boolean useAsteroidsMode;
	
	// Audio
	private static AudioInputStream audioIn;
	private Clip clip;
	private FloatControl gainControl; 

	// The PlanetaryBody has a mutex lock, needed for handing off permission to alter
	// the body's position, whether by gravity, inertia, or collision.
	public Object lock = new Object();
		
	/*
	 * The Getters and Setters
	 * =======================
	 */
	
	public String getSatelliteName() {
		return this.satelliteName;
	}
	
	public void setSatelliteName(String name) {
		this.satelliteName = name;
	}
	
	public float getMass() {
		return this.mass;
	}
	
	public void setMass(float mass) {
		this.mass = mass;
	}
		
	public double getX() {
		return xPos;
	}
	
	public void setX(double x ) {
		this.xPos = x;
	}
	
	public double getY() {
		return this.yPos;
	}
	
	public void setY(double y) {
		this.yPos = y;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}
	
	public boolean isUseGravity() {
		return useGravity;
	}

	public void setUseGravity(boolean useGravity) {
		this.useGravity = useGravity;
	}

	public boolean isUseInertia() {
		return useInertia;
	}

	public void setUseInertia(boolean useInertia) {
		this.useInertia = useInertia;
	}

	public double getVelocityX() {
		return velocityX;
	}

	public void setVelocityX(double inertiaX) {
		this.velocityX = inertiaX;
	}

	public double getVelocityY() {
		return velocityY;
	}

	public void setVelocityY(double inertiaY) {
		this.velocityY = inertiaY;
	}

	public Rectangle getCollider() {
		return collider;
	}

	public void setCollider(Rectangle collider) {
		this.collider = collider;
	}	
	
	public Clip getClip() {
		return clip;
	}

	public void setClip(Clip thisclip) {
		clip = thisclip;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public boolean isUseSound() {
		return useSound;
	}

	public void setUseSound(boolean useSound) {
		this.useSound = useSound;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	private double getGravitydivisor() {
		return gravitydivisor;
	}

	public void setGravityDivisor(double gravitydivisor) {
		this.gravitydivisor = gravitydivisor;
	}	
	
	/*
	 *  The Good Stuff
	 *  ==============
	 */
	
	// Default constructor
	public PlanetaryBody() {
	}
	
	// Constructor
	public PlanetaryBody(String name, float mass, int radius, double xPos, double yPos, boolean randomizeXVel, boolean randomizeYVel) {
		setUseSound(true);
		loadAudio();
				
		this.satelliteName = name;
		//this.setColor(colors[getRandomNumberInRange(0, colors.length - 1)]);
		this.color = Color.WHITE;
		
		this.mass = mass;
		this.setRadius(radius);
		this.xPos = xPos;
		this.yPos = yPos;
		
		// Randomize initial velocity.
		// Note: For some reason, this only works during initialization. 
		// TODO: Get randomizeVelocity working for resets.
		if (randomizeXVel == true) {
			this.setVelocityX(randomizeVelocity());
		} else {
			this.setVelocityX(Main.initialX);
		}
		
		if (randomizeYVel == true) {
			this.setVelocityY(randomizeVelocity());
		}else {
			this.setVelocityY(Main.initialY);
		}
				
		this.setCollider(new Rectangle((int)xPos, (int)yPos, (int)(radius * 1.5), (int)(radius * 1.5)));
		
		setUseGravity(true);
		setUseInertia(true);
		setKeepAlive(true);
		setUseCollisions(false);
	}

	// Velocity Randomizer, used during initialization. (Does not work while object thread is running.)
	public double randomizeVelocity() {
		Random rand = new Random();
		double randVel = (rand.nextDouble() * 2 - 1) * 0.5f;
		return randVel;
	}

	// The main Run thread.
	public void run() {
		while(isKeepAlive() == true) {
			
			if (isUseAsteroidsMode() == true) {
				asteroidsMode(); // experimental
			}
			
			// Get the location for later calculating velocity.
			double firstX = this.getX();
			double firstY = this.getY();	
			
			// Detect collision, and if there is a collision, bounce.
			if (useCollisions == true) {
				detectCollisions();
			}
			
			// Apply the inertia calculate from the last loop, or from the randomizer.
			if (isUseInertia() == true) {
				applyInertia();
			}

			// Do gravity
			if (isUseGravity() == true) {
				calculateAndApplyGravity();
			}	
			
			// Wait for 1/2 frame
			try {
				Thread.sleep(8);
			}
			catch(InterruptedException e) {
				System.out.println(e);
			}
						
			if (isUseInertia() == true) {
				// Get position again, for calculating velocity, to be applied as inertia in the next loop iteration.
				setVelocityForInertia(firstX, firstY);
				//System.out.println(this.getPlanetName() + " | " + getInertiaX() + " | " + getInertiaY());
			}
		}
	}

	// The Velocity is measured by the difference in the last known position (should be about 8ms) and this one.
	// This is called after inertia, gravity, and collision physics have been applied for a loop iteration.
	private void setVelocityForInertia(double firstX, double firstY) {
		double lastX = this.getX();
		double lastY = this.getY();
		setVelocityX(lastX - firstX) ;
		setVelocityY(lastY - firstY) ;
	}

	// Inertia is applied to the position before gravity and bounce collision physics have been applied.
	private void applyInertia() {
		synchronized(lock) {
			this.xPos = this.xPos + getVelocityX();
			this.yPos = this.yPos + getVelocityY();
			this.getCollider().setLocation((int)xPos, (int)yPos); // Move the collider too.
		}
	}

	// Iterate through all bodies and adjust their positions using self's mass.
	private synchronized void calculateAndApplyGravity() {
		for (PlanetaryBody p : Main.getSatellites())
		{
			if (p != this){
				synchronized (p.lock) {
												
					// Calculate the distance between self and p.
					double distance = getCombinedDistance(this.xPos, this.yPos, p.getX(), p.getY());
					
					// Calculate gravitational pull of body p on this body.
					// Newton's equation describes the pull between two objects, but for our purposes
					// we want to describe the force that this object exerts on another, assuming
					// the other also does the same.
					double pullOfThisMass = gravitationalConstant * (this.mass / (distance*distance));
					
					// Get the direction from p to self.
					// This can and should be negative sometimes.
					double xDir = p.getX() -this.xPos;
					double yDir =  p.getY() - this.yPos;
										
					// Get the absolute value of X and Y distances from p to self.
					// This should always be positive.
					double xDist = Math.abs(p.getX() - this.xPos);
					double yDist = Math.abs(p.getY() - this.yPos);
					
					// Update P's coordinates according to gravity.
					if (xDist > (this.getRadius() + p.getRadius())
							&& (yDist > this.getRadius() + p.getRadius())) {
						
						// gravitX and gravityY calculate the pull of this mass, multiplied by a line from self to p.
						double gravityX = (double)(p.getX() + (pullOfThisMass * (xDir/getGravitydivisor())));
						double gravityY = (double)(p.getY() + (pullOfThisMass * (yDir/getGravitydivisor())));
						
						if (!p.getSatelliteName().equals("Earth"))
						{
							p.setX(gravityX);
							p.setY(gravityY);
							p.getCollider().setLocation((int)gravityX, (int)gravityY); // Move the collider too.
						}
					}
				}
			}
		}
	}

	private void detectCollisions() {
		for (PlanetaryBody pb : Main.getSatellites()) {
			if (pb != this) {
				synchronized(pb.lock) {
					double distanceX = xPos - pb.getX();
					double distanceY = yPos - pb.getY();
					
					if (distanceX < (this.getRadius() + pb.getRadius()/2) && distanceX > (this.getRadius() + pb.getRadius())*(7/8)
							&& distanceY < (this.getRadius() + pb.getRadius()/2) && distanceY > (this.getRadius() + pb.getRadius())*(7/8)){
						if (this.collider.intersects(pb.collider)) {
							collisionBounce(this, pb);
						}
					}
				}
			}
		}
	}

	// This method based on Christopher Lis' util-elastic-collision.js.
	private void collisionBounce(PlanetaryBody pbSelf, PlanetaryBody pbOther) {
		double velocityDifferentialX = pbSelf.getVelocityX() - pbOther.getVelocityX();
		double velocityDifferentialY = pbSelf.getVelocityY()- pbOther.getVelocityY();
		double distanceX = pbSelf.getX() - pbOther.getX();
		double distanceY = pbSelf.getY() - pbOther.getY();
		
		if (velocityDifferentialX * distanceX + velocityDifferentialY * distanceY >= 0) {
			double angleBetweenBodies = -Math.atan2(pbOther.getY() - pbSelf.getY(), pbOther.getX() - pbSelf.getX());
				
		//	System.out.println(angleBetweenBodies);
	   //     System.out.println("first velocities " + pbSelf.satelliteName + ": " + pbSelf.getVelocityX() + " | " + pbSelf.getVelocityY());
	   //     System.out.println("first velocities " + pbOther.satelliteName + ": " + pbOther.getVelocityX() + " | " + pbOther.getVelocityY());

	        double mass1 = pbSelf.getMass();
	        double mass2 = pbOther.getMass();

	        // Angular Velocity before equation
	     //     double[] u1 = rotate(pbSelf.getVelocityX(), pbSelf.getVelocityY(), (double)angleBetweenBodies);
	     //     double[] u2 = rotate(pbOther.getVelocityX(), pbOther.getVelocityY(), (double)angleBetweenBodies);
	        
	        double[] u1 = new double[2];
	        u1[0] = pbSelf.getVelocityX();
	        u1[1] = pbSelf.getVelocityY();
	        
	        double[] u2 = new double[2];
	        u2[0] = pbOther.getVelocityX();
	        u2[1] = pbOther.getVelocityY();
	        
	     //   System.out.println("Angular Vel before equation. " + pbSelf.satelliteName + ": " + u1[0] + " | " + u1[1]  );
	     //   System.out.println("Angular Vel before equation. " + pbOther.satelliteName + ": " + u2[0] + " | " + u2[1]  );
	        
	    //    // Velocity after 1d collision equation
	          double[] v1 = new double[2];
	          v1[0] = (double) (u1[0] * (mass1 - mass2) / (mass1 + mass2) + u2[0] * 2 * mass2 / (mass1 + mass2));
	          v1[1] = u1[1];
	          	
	          double[] v2 = new double[2];
	          v2[0] = (double) (u2[0] * (mass1 - mass2) / (mass1 + mass2) + u1[0] * 2 * mass2 / (mass1 + mass2));
	          v2[1] = u2[1];
	        
	        // Final velocity after rotating axis back to original location
	    //    double[] vFinal1 = rotate(v1[0], v1[1], (double)-angleBetweenBodies);
	    //    double[] vFinal2 = rotate(v2[0], v2[1], (double)-angleBetweenBodies);
	        
	    //    System.out.println("Angular Vel after rotating axis back to original location " + pbSelf.satelliteName + ": " + vFinal1[0] + " | " + vFinal1[1]  );
	    //    System.out.println("Angular Vel after rotating axis back to original location " + pbOther.satelliteName + ": " + vFinal2[0] + " | " + vFinal2[1]  );
	      
        	double[] vFinal1 = v1;
        	double[] vFinal2 = v2;
	        
	        // Swap velocities for realistic bounce effect
	        // For the sake of centering, in lieu of a camera object, the Earth does not move. So...
	        // If I am the Earth, only bounce pbOther.
	        if (this.getSatelliteName().equals("Earth")) {
	        	synchronized(pbOther.lock) {
			        pbOther.setVelocityX(vFinal2[0]);
			        pbOther.setVelocityY(vFinal2[1]);
	        	}

	        } else if (pbOther.getSatelliteName().equals("Earth")) {
	        	
	        	// But if pbOther is the Earth, then I am not. Bounce only me, and not the Earth.
	        	synchronized(this.lock) {
		        	pbSelf.setVelocityX(vFinal1[0]);
			        pbSelf.setVelocityY(vFinal1[1]);
	        	}
	        } else {
	        	
	        	// If neither of us is the Earth, bounce each other.
	        	synchronized(this.lock) {
		        	pbSelf.setVelocityX(vFinal1[0]);
			        pbSelf.setVelocityY(vFinal1[1]);
	        	}

	        	synchronized(pbOther.lock) {
			        pbOther.setVelocityX(vFinal2[0]);
			        pbOther.setVelocityY(vFinal2[1]);	
	        	}
	
		        
		      //  System.out.println("Collision Velocity: " + pbSelf.getSatelliteName() +"|" + vFinal1[0] + "|" + vFinal1[1]);
		      //  System.out.println("Collision Velocity: " + pbOther.getSatelliteName() +"|" + vFinal2[0] + "|" + vFinal2[1]);
	        }
	        
	        flashColor();
	        
	        if (isUseSound() == true){
		        playHitSound(pbSelf, pbOther);
	        }
		}
	}
	
	// Calculates a rotation by accepting two velocities and an angle, and returns a 2D double.
	public double[] rotate(double velocityX, double velocityY, double angle) {
	    double[] rotatedVelocities = new double[2];
	    rotatedVelocities[0] = (double) (velocityX * Math.cos(angle) - velocityY * Math.sin(angle));
		rotatedVelocities[1] = (double) (velocityX * Math.sin(angle) + velocityY * Math.cos(angle));
		
	    return rotatedVelocities;
	}
	
	public static double getCombinedDistance(double x1, double y1, double x2, double y2) {
	    double dx = x2 - x1;
	    double dy = y2 - y1;
	    return Math.sqrt(dx * dx + dy * dy); 
	}

	public void loadAudio() {
		Thread audioThread = new Thread() {
			public void run() {
				try {
					File soundFile = new File("src/sounds/karma-ron_orch-002-boom.wav");
					audioIn = AudioSystem.getAudioInputStream(soundFile);
				} catch (UnsupportedAudioFileException e) {
					e.printStackTrace();
					System.out.println("UNSUPPORTED AUDIO FILE TYPE");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("I/O EXCEPTION while trying to assign 'audioIn'. Wrong path?");
				}
				
				try {
					setClip(AudioSystem.getClip());
					getClip().open(audioIn);			        
				} catch (LineUnavailableException e) {
					e.printStackTrace();
					System.out.println("FAILED TO GET CLIP");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("I/O EXCEPTION while trying to get Clip.");
				}
				
				gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			}
		};
		
		audioThread.run();
	}
	
	private void playHitSound(PlanetaryBody pbSelf, PlanetaryBody pbOther) {
		// Play the "hit" sound.
		if(!clip.isActive()) {
			clip.setFramePosition(0);  // Must always rewind!
			
			// set the gain scaled to the velocity of the hit.
			double gain;
			float dB;
			if (pbOther.getSatelliteName()=="Earth") {
				gain = ((Math.abs(pbSelf.getVelocityX()) + Math.abs(pbSelf.getVelocityY()) * (mass/100)) /2 );
				dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
			}
			else {
				gain = ((Math.abs(pbOther.getVelocityX()) + Math.abs(pbOther.getVelocityY()) * (pbOther.getMass()/100)) /2);
				dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
			}
			 
			if (dB < 6 && gain > 0.2) {
				gainControl.setValue(dB);
				clip.start();
			} else if (dB > 6){
				gainControl.setValue(6);
				clip.start();
			}
			//System.out.println("Gain: " + gain + "| Decibels" + dB);
		}	
	}

	private void flashColor() {
		Thread thisThread = new Thread() {
			public void run() {
				if (!satelliteName.equals("Earth")) {
					color = Color.CYAN;
					try {
						sleep(32);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					color = Color.WHITE ;
				}
			}
		};
		
		thisThread.start();
	}

	public static int getRandomNumberInRange(int min, int max) {
		if (min >= max) 
			throw new IllegalArgumentException("max must be greater than min");
		
		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}
	
	// EXPERIMENTAL
	// Make objects jump to the opposite side of the screen when they go off screen, like in Asteroids or Pac-Man.

	public void asteroidsMode() {
		if (this.xPos > Main.getOrbitFrame().getWidth()) {
			this.xPos = 0;
		} else if (this.xPos <= 0) {
			this.xPos = Main.getOrbitFrame().getWidth();
		}
		
		if (this.yPos > Main.getOrbitFrame().getHeight()) {
			this.yPos = 0;
		} else if (this.yPos <= 0) {
			this.yPos = Main.getOrbitFrame().getHeight();
		}
	}

	public boolean isUseAsteroidsMode() {
		return useAsteroidsMode;
	}

	public void setUseAsteroidsMode(boolean useAsteroidsMode) {
		this.useAsteroidsMode = useAsteroidsMode;
	}

	public void setUseCollisions(boolean b) {
		this.useCollisions = b;
		
	}
}
