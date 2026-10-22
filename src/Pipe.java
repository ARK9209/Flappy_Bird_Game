import java.awt.*;

/** One top-and-bottom obstacle pair. */
public class Pipe {
    public static final int WIDTH = 76;
    private final int topHeight, bottomY, playHeight; private int x; private boolean passed;
    public Pipe(int x,int topHeight,int gap,int playHeight){this.x=x;this.topHeight=topHeight;this.bottomY=topHeight+gap;this.playHeight=playHeight;}
    public void update(int speed){x-=speed;}
    public void draw(Graphics2D g){drawPart(g,0,topHeight,true);drawPart(g,bottomY,playHeight-bottomY,false);}
    private void drawPart(Graphics2D g,int y,int h,boolean top){
        int capY=top?y+h-22:y, depth=15;
        g.setColor(new Color(5,35,42,90));g.fillRoundRect(x+10,y+11,WIDTH,h,12,12);
        g.setPaint(new GradientPaint(x,y,new Color(92,219,151),x+WIDTH,y,new Color(17,101,102)));g.fillRoundRect(x,y,WIDTH,h,10,10);
        g.setColor(new Color(11,75,80));g.fillPolygon(new int[]{x+WIDTH,x+WIDTH+depth,x+WIDTH+depth,x+WIDTH},new int[]{y,y+depth,y+h+depth,y+h},4);
        g.setColor(new Color(205,255,193,125));g.fillRoundRect(x+9,y+6,9,Math.max(0,h-12),6,6);
        g.setColor(new Color(14,74,75));g.setStroke(new BasicStroke(2));g.drawRoundRect(x,y,WIDTH,h,10,10);
        g.setPaint(new GradientPaint(x-7,capY,new Color(128,239,166),x+WIDTH+7,capY,new Color(22,123,111)));g.fillRoundRect(x-7,capY,WIDTH+14,22,7,7);
        g.setColor(new Color(12,67,71));g.drawRoundRect(x-7,capY,WIDTH+14,22,7,7);
        g.setColor(new Color(16,85,82));g.fillPolygon(new int[]{x+WIDTH+7,x+WIDTH+7+depth,x+WIDTH+7+depth,x+WIDTH+7},new int[]{capY,capY+depth,capY+22+depth,capY+22},4);
    }
    public Rectangle getTopBounds(){return new Rectangle(x,0,WIDTH,topHeight);} public Rectangle getBottomBounds(){return new Rectangle(x,bottomY,WIDTH,playHeight-bottomY);} public int getX(){return x;} public boolean isPassed(){return passed;} public void setPassed(boolean passed){this.passed=passed;}
}
