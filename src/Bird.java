import java.awt.*;
import java.awt.geom.AffineTransform;

/** The player character: physics and Java2D drawing stay together for clarity. */
public class Bird {
    public static final int WIDTH = 46, HEIGHT = 36;
    private static final double GRAVITY = 0.42, FLAP_POWER = -7.4;
    private final int x;
    private double y, velocity, wingPhase;
    public Bird(int x, int y) { this.x = x; reset(y); }
    public void reset(int startY) { y = startY; velocity = 0; wingPhase = 0; }
    public void update() { velocity += GRAVITY; velocity = Math.min(velocity, 10); y += velocity; wingPhase += 0.32; }
    public void flap() { velocity = FLAP_POWER; wingPhase = 0; }
    public Rectangle getBounds() { return new Rectangle(x + 4, (int)y + 3, WIDTH - 8, HEIGHT - 6); }
    public int getY() { return (int)y; }
    public double getVelocity() { return velocity; }
    public void draw(Graphics2D g) {
        AffineTransform old = g.getTransform(); int py=(int)y;
        g.rotate(Math.max(-.35, Math.min(.65, velocity * .055)), x + WIDTH / 2.0, y + HEIGHT / 2.0);
        g.setColor(new Color(7,38,40,80)); g.fillOval(x+5,py+HEIGHT+5,WIDTH-3,8);
        // Tail feathers sit behind the body, giving the bird a clear silhouette.
        g.setColor(new Color(37,121,156));g.fillPolygon(new int[]{x+2,x-13,x+4},new int[]{py+19,py+10,py+31},3);g.fillPolygon(new int[]{x+7,x-8,x+12},new int[]{py+24,py+28,py+35},3);
        g.setPaint(new GradientPaint(x,py,new Color(87,209,237),x,py+HEIGHT,new Color(21,117,181)));g.fillOval(x,py,WIDTH,HEIGHT);
        g.setColor(new Color(9,65,116));g.setStroke(new BasicStroke(2f));g.drawOval(x,py,WIDTH,HEIGHT);
        g.setColor(new Color(170,239,249,145));g.fillOval(x+7,py+5,21,10);
        int wingY=py+14+(int)(Math.sin(wingPhase)*4);g.setPaint(new GradientPaint(x+5,wingY,new Color(48,151,206),x+29,wingY+17,new Color(13,77,151)));g.fillOval(x+4,wingY,25,17);
        g.setColor(new Color(8,61,128));g.setStroke(new BasicStroke(1.6f));g.drawOval(x+4,wingY,25,17);g.drawLine(x+9,wingY+7,x+25,wingY+13);g.drawLine(x+10,wingY+11,x+22,wingY+16);
        // Eye ring, iris, and highlight create a more expressive focal point.
        g.setColor(Color.WHITE);g.fillOval(x+27,py+5,15,15);g.setColor(new Color(27,54,66));g.fillOval(x+33,py+8,7,8);g.setColor(Color.WHITE);g.fillOval(x+35,py+9,2,2);
        Polygon beak=new Polygon(new int[]{x+39,x+56,x+39},new int[]{py+17,py+23,py+29},3);g.setPaint(new GradientPaint(x+39,py+17,new Color(255,207,57),x+53,py+28,new Color(239,112,27)));g.fillPolygon(beak);g.setColor(new Color(151,76,24));g.drawPolygon(beak);g.drawLine(x+40,py+23,x+52,py+23);
        g.setTransform(old);
    }
}
