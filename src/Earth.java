// noot noot
import java.awt.Color;
import java.awt.Rectangle;

public class Earth extends PlanetaryBody{
	private static volatile Earth instance;

	// Constructor
	public Earth() {
		loadAudio();
		this.setColor(Color.WHITE);
		this.setSatelliteName("Earth");
		this.setMass((long)5.972 * (10^24));
		this.setRadius(60);
		this.setUseGravity(true);
		this.setUseInertia(true);
		this.setKeepAlive(true);
		this.setCollider(new Rectangle((int)getX(), (int)getY(), (int)getRadius() * (1 + (1/2)), (int)getRadius() * (1 + (1/2))));
	}

	// Use getInstance to ensure there is only one instance at a time.
	public static synchronized Earth getInstance() {
		
		if(instance==null) {
			System.out.println("======== \n NEW EARTH HERE! \n=========");
			instance = new Earth();
		}
		return instance;
	}
	
	// The reset so Singleton can be intentionally circumvented.
	public static void reset() {
		instance = new Earth();
	}
}
