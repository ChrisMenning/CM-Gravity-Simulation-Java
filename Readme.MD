   This program simulates gravity, based on Newtonian physics, without the benefit of a physics engine to work with.
   Each PlanetaryBody exerts a gravitational pull on all others, proportional to its mass, and its distance from each other.
   
   Watch for a few minutes as a randomly generated array of satellites dance around the Earth and moon.
   Keep an eye out for the following rare emergent, unscripted behaviors:
   * Satellites orbiting other satellites.
   * Clustering / Glob / Strand formation
   
   This project utilizes the following concepts learned in 21751, Java 4, at NWTC:
   * Multi-threading - Each PlanetaryBody extends Thread. There are also Threads for sound effects and screen refreshing.
   * Synchronization - Mutex locks help balance all of the concurrent physics calculations across the entire array of objects.
   * I/O - Music and collision sound effect are loaded from resources.
   * Design Patterns - Earth object uses a Singleton design pattern.
   
   Gravity physics based heavily on Newton's theorem F = g(m1*m2)/d^2.
   Inertia physics created through my own guess-and-test work on this project.
   
   Collision detection and bounce reaction based on util-elastic-collision.js by Christopher Lis, found here: 
   https://gist.github.com/christopher4lis/f9ccb589ee8ecf751481f05a8e59b1dc
   
   TODO: 
   * Explore using BigDecimal (and its associated math methods) so the very big numbers don't overflow.