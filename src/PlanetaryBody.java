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
	private long mass; 
	private double radius;
	private float xPos;
	private float yPos;
	private float velocityX;
	private float velocityY;
	private Rectangle collider;
	private Color color;
	
	// Behavior modifiers
	private boolean useGravity;
	private boolean useInertia;
	private boolean useSound;
	private boolean keepAlive;
	private double gravityDiviser = 1000;
	
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
	
	public long getMass() {
		return this.mass;
	}
	
	public void setMass(long mass) {
		this.mass = mass;
	}
		
	public float getX() {
		return xPos;
	}
	
	public void setX(float x ) {
		this.xPos = x;
	}
	
	public float getY() {
		return this.yPos;
	}
	
	public void setY(float y) {
		this.yPos = y;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
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

	public float getVelocityX() {
		return velocityX;
	}

	public void setVelocityX(float inertiaX) {
		this.velocityX = inertiaX;
	}

	public float getVelocityY() {
		return velocityY;
	}

	public void setVelocityY(float inertiaY) {
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

	private double getGravityDiviser() {
		return gravityDiviser;
	}

	public void setGravityDiviser(double gravityDiviser) {
		this.gravityDiviser = gravityDiviser;
	}	
	
	/*
	 *  The Good Stuff
	 *  ==============
	 */
	
	// Default constructor
	public PlanetaryBody() {
	}
	
	// Constructor
	public PlanetaryBody(String name, long mass, double radius, float xPos, float yPos, boolean randomizeXVel, boolean randomizeYVel) {
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
	}

	// Velocity Randomizer, used during initialization. (Does not work while object thread is running.)
	public float randomizeVelocity() {
		Random rand = new Random();
		float randVel = (rand.nextFloat() * 2 - 1) * 0.5f;
		return randVel;
	}

	// The main Run thread.
	public void run() {
		while(isKeepAlive() == true) {
			
			// Get the location for later calculating velocity.
			float firstX = this.getX();
			float firstY = this.getY();
			
			// Apply the inertia calculate from the last loop, or from the randomizer.
			if (isUseInertia() == true) {
				applyInertia();
			}
			
			// Detect collision, and if there is a collision, bounce.
			detectCollisions();
			
			// Wait for 1/2 frame
			try {
				Thread.sleep(8);
			}
			catch(InterruptedException e) {
				System.out.println(e);
			}
			
			// Do gravity
			if (isUseGravity() == true) {
				calculateAndApplyGravity();
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
	private void setVelocityForInertia(float firstX, float firstY) {
		float lastX = this.getX();
		float lastY = this.getY();
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
					double pullOfThisMass = gravitationalConstant * ((this.mass) / (distance*distance));
					
					// Get the direction from p to self.
					// This can and should be negative sometimes.
					float xDir = p.getX() -this.xPos;
					float yDir =  p.getY() - this.yPos;
										
					// Get the absolute value of X and Y distances from p to self.
					// This should always be positive.
					float xDist = Math.abs(p.getX() - this.xPos);
					float yDist = Math.abs(p.getY() - this.yPos);
					
					// Update P's coordinates according to gravity.
					if (xDist > this.getRadius()/2 && xDist > p.getRadius()/2
							|| yDist > this.getRadius()/2 && yDist > p.getRadius()/2){
						
						// gravitX and gravityY calculate the pull of this mass, multiplied by a line from self to p.
						float gravityX = (float)(p.getX() + (pullOfThisMass * (xDir/getGravityDiviser())));
						float gravityY = (float)(p.getY() + (pullOfThisMass * (yDir/getGravityDiviser())));
						
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
					if (this.getCollider().intersects(pb.getCollider())){
						//System.out.println(this.getPlanetName() + " collided with " + pb.getPlanetName());
						collisionBounce(this, pb);
					}
				}
			}
		}
	}

	// This method based on Christopher Lis' util-elastic-collision.js.
	private void collisionBounce(PlanetaryBody pbSelf, PlanetaryBody pbOther) {
		float velocityDifferentialX = pbSelf.velocityX - pbOther.velocityX;
		float velocityDifferentialY = pbSelf.velocityY - pbOther.velocityY;
		float distanceX = pbSelf.getX() - pbOther.getX();
		float distanceY = pbSelf.getY() - pbOther.getY();
		
		if (velocityDifferentialX * distanceX + velocityDifferentialY * distanceY >= 0) {
			double angleBetweenBodies = -Math.atan2(pbOther.getY() - pbSelf.getY(), pbOther.getX() - pbSelf.getX());
			
	        double mass1 = pbSelf.getMass();
	        double mass2 = pbOther.getMass();

	        // Angular Velocity before equation
	        float[] u1 = rotate(pbSelf.getVelocityX(), pbSelf.getVelocityY(), (float)angleBetweenBodies);
	        float[] u2 = rotate(pbOther.getVelocityX(), pbOther.getVelocityY(), (float)angleBetweenBodies);
	        
	        // Velocity after 1d collision equation
	        float[] v1 = new float[2];
	        v1[0] = (float) (u1[0] * (mass1 - mass2) / (mass1 + mass2) + u2[0] * 2 * mass2 / (mass1 + mass2));
	        v1[1] = u1[1];
	        	
	        float[] v2 = new float[2];
	        v2[0] = (float) (u2[0] * (mass1 - mass2) / (mass1 + mass2) + u1[0] * 2 * mass2 / (mass1 + mass2));
	        v2[1] = u2[1];
	        
	        // Final velocity after rotating axis back to original location
	        float[] vFinal1 = rotate(v1[0], v1[1], (float)-angleBetweenBodies);
	        float[] vFinal2 = rotate(v2[0], v2[1], (float)-angleBetweenBodies);
	        
	        // Swap velocities for realistic bounce effect
	        
	        // For the sake of centering, in lieu of a camera object, the Earth does not move. So...
	        // If I am the Earth, only bounce pbOther.
	        if (this.getSatelliteName().equals("Earth")) {
		        pbOther.setVelocityX(vFinal2[0]);
		        pbOther.setVelocityY(vFinal2[1]);
	        } else if (pbOther.getSatelliteName().equals("Earth")) {
	        	
	        	// But if pbOther is the Earth, then I am not. Bounce only me, and not the Earth.
	        	pbSelf.setVelocityX(vFinal1[0]);
		        pbSelf.setVelocityY(vFinal1[1]);
	        } else {
	        	
	        	// If neither of us is the Earth, bounce each other.
	        	pbSelf.setVelocityX(vFinal1[0]);
		        pbSelf.setVelocityY(vFinal1[1]);
		        pbOther.setVelocityX(vFinal2[0]);
		        pbOther.setVelocityY(vFinal2[1]);
	        }
	        
	        flashColor();
	        
	        if (isUseSound() == true){
		        playHitSound(pbSelf, pbOther);
	        }
		}
	}
	
	// Calculates a rotation by accepting two velocities and an angle, and returns a 2D float.
	public float[] rotate(float velocityX, float velocityY, float angle) {
	    float[] rotatedVelocities = new float[2];
	    rotatedVelocities[0] = (float) (velocityX * Math.cos(angle) - velocityY * Math.sin(angle));
		rotatedVelocities[1] = (float) (velocityX * Math.sin(angle) + velocityY * Math.cos(angle));
	    
	    return rotatedVelocities;
	}
	
	public static double getCombinedDistance(float x1, float y1, float x2, float y2) {
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
		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}

		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}
}
