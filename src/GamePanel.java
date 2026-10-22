import javax.swing.*;
import javax.swing.AbstractAction;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.List;

/** Main view and small, single-timer game loop. Coordinates use an 800x600 logical canvas. */
public class GamePanel extends JPanel {
    public static final int W=800,H=600,GROUND_Y=510,PIPE_GAP=178;
    private static final int SUNLIGHT=0, RAIN=1, SUNSET=2, NIGHT=3;
    private static final int[] BUILDING_WIDTHS={62,78,52,88}, FAR_BUILDING_TOPS={232,195,214,246}, NEAR_BUILDING_TOPS={155,104,131,185};
    private static final Color[] RAIN_DROP_COLORS={new Color(197,223,255,110),new Color(197,223,255,128),new Color(197,223,255,146),new Color(197,223,255,164),new Color(197,223,255,182),new Color(197,223,255,200),new Color(197,223,255,218),new Color(197,223,255,236),new Color(197,223,255,254)};
    private static final Color RAIN_PUDDLE_COLOR=new Color(164,206,238,90), RAIN_SPLASH_COLOR=new Color(110,146,180,88);
    private static final Stroke RAIN_DROP_STROKE=new BasicStroke(1.5f), RAIN_PUDDLE_STROKE=new BasicStroke(1.2f);
    private final Bird bird=new Bird(145,260); private final List<Pipe> pipes=new ArrayList<>(); private final Random random=new Random();
    private final Timer timer; private final ScoreManager scores=new ScoreManager(); private final JTextField nameField=new JTextField();
    private GameState state=GameState.MENU; private int score, worldOffset, cloudOffset, jungleOffset; private String name=""; private String notice=""; private boolean scoreSaved, scoreSaving; private List<ScoreManager.DatabaseScore> leaders=Collections.emptyList();
    private final UiButton play=new UiButton("PLAY",300,374,200,54,new Color(235,115,69));
    private final UiButton how=new UiButton("HOW TO PLAY",300,440,200,44,new Color(56,142,172));
    private final UiButton high=new UiButton("HIGH SCORES",300,496,200,44,new Color(79,158,125));
    private final UiButton resume=new UiButton("RESUME",300,300,200,48,new Color(56,142,172)), restart=new UiButton("RESTART",300,360,200,48,new Color(235,115,69)), menu=new UiButton("MAIN MENU",300,420,200,48,new Color(79,158,125));
    private final UiButton replay=new UiButton("REPLAY",194,454,190,48,new Color(235,115,69)), save=new UiButton("SAVE SCORE",416,454,190,48,new Color(79,158,125)), board=new UiButton("LEADERBOARD",300,510,200,42,new Color(56,142,172)), back=new UiButton("BACK",300,520,200,44,new Color(56,142,172));
    public GamePanel(){ setPreferredSize(new Dimension(W,H)); setLayout(null); setFocusable(true); setBackground(Color.BLACK); setupNameField(); setupInput(); timer=new Timer(33,e->tick()); timer.start(); }
    private void setupNameField(){nameField.setBounds(300,337,200,38);nameField.setVisible(false);nameField.setFont(new Font("SansSerif",Font.PLAIN,16));nameField.setForeground(Color.WHITE);nameField.setCaretColor(Color.WHITE);nameField.setBackground(new Color(27,71,91));nameField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(180,237,244),2),BorderFactory.createEmptyBorder(3,10,3,10)));nameField.addActionListener(e->saveScore());add(nameField);}
    private void setupInput(){
        // Window-level bindings still work after a Swing child (such as the name field)
        // receives focus. Relying on a KeyListener alone left the menu feeling unresponsive
        // on some systems.
        ActionMap actions=getActionMap(); setWindowShortcutsEnabled(true);
        actions.put("primary",new AbstractAction(){public void actionPerformed(ActionEvent e){if(state==GameState.PLAYING)bird.flap();else if(state==GameState.MENU||state==GameState.GAME_OVER)startGame();else if(state==GameState.PAUSED){state=GameState.PLAYING;timer.setDelay(16);timer.start();}repaint();}});
        actions.put("pause",new AbstractAction(){public void actionPerformed(ActionEvent e){if(state==GameState.PLAYING){state=GameState.PAUSED;timer.stop();}else if(state==GameState.PAUSED){state=GameState.PLAYING;timer.setDelay(16);timer.start();}repaint();}});
        actions.put("leaderboard",new AbstractAction(){public void actionPerformed(ActionEvent e){if(state==GameState.MENU||state==GameState.GAME_OVER)showBoard();repaint();}});
        MouseAdapter mouse=new MouseAdapter(){public void mousePressed(MouseEvent e){handleClick(e.getX(),e.getY());} public void mouseMoved(MouseEvent e){for(UiButton b:buttons())b.setHover(b.contains(e.getX(),e.getY()));repaint();}}; addMouseListener(mouse); addMouseMotionListener(mouse);
    }
    private void setWindowShortcutsEnabled(boolean enabled){InputMap keys=getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);KeyStroke[] shortcuts={KeyStroke.getKeyStroke("SPACE"),KeyStroke.getKeyStroke("ENTER"),KeyStroke.getKeyStroke("ESCAPE"),KeyStroke.getKeyStroke("L")};String[] actions={"primary","primary","pause","leaderboard"};for(int i=0;i<shortcuts.length;i++){if(enabled)keys.put(shortcuts[i],actions[i]);else keys.remove(shortcuts[i]);}}
    private List<UiButton> buttons(){return Arrays.asList(play,how,high,resume,restart,menu,replay,save,board,back);}
    private void handleClick(int x,int y){requestFocusInWindow();if(scoreSaving)return; if(state==GameState.PLAYING){bird.flap();return;} if(state==GameState.MENU){if(play.contains(x,y))startGame();else if(high.contains(x,y))showBoard();else if(how.contains(x,y))notice="SPACE or click to flap. Avoid pipes and keep flying.";} else if(state==GameState.PAUSED){if(resume.contains(x,y)){state=GameState.PLAYING;timer.setDelay(16);timer.start();}else if(restart.contains(x,y))startGame();else if(menu.contains(x,y)){state=GameState.MENU;timer.setDelay(33);timer.start();}} else if(state==GameState.GAME_OVER){if(replay.contains(x,y))startGame();else if(save.contains(x,y))saveScore();else if(board.contains(x,y))showBoard();} else if(state==GameState.LEADERBOARD&&back.contains(x,y)){state=GameState.MENU;setWindowShortcutsEnabled(true);timer.setDelay(33);timer.start();} repaint();}
    private void startGame(){score=0;name="";notice="";scoreSaved=false;scoreSaving=false;nameField.setEnabled(true);nameField.setText("");nameField.setVisible(false);pipes.clear();bird.reset(260);addPipe(850);state=GameState.PLAYING;setWindowShortcutsEnabled(true);timer.setDelay(16);timer.start();requestFocusInWindow();}
    private void addPipe(int x){int top=95+random.nextInt(190);pipes.add(new Pipe(x,top,currentPipeGap(),GROUND_Y));}
    private int difficultyLevel(){return score/5;}
    private int currentPipeSpeed(){return Math.min(10,4+difficultyLevel());}
    private int currentPipeGap(){return Math.max(122,PIPE_GAP-difficultyLevel()*6);}
    /** Rain at 5, sunset at 10, night at 15, sunlight at 20; repeat every 20 points. */
    private int cityTheme(){return score<5?SUNLIGHT:(score/5)%4;}
    private void tick(){cloudOffset=(cloudOffset+2)%(W*4);jungleOffset=(jungleOffset+3)%(W*8);if(state==GameState.PLAYING){int speed=currentPipeSpeed();worldOffset=(worldOffset+speed)%40;bird.update();for(Pipe p:pipes){p.update(speed);if(!p.isPassed()&&p.getX()+Pipe.WIDTH<145){p.setPassed(true);score++;}} if(pipes.get(pipes.size()-1).getX()<470)addPipe(W+30);if(!pipes.isEmpty()&&pipes.get(0).getX()<-Pipe.WIDTH-20)pipes.remove(0);if(bird.getY()<0||bird.getY()+Bird.HEIGHT>GROUND_Y||collides())gameOver();} repaint();}
    private boolean collides(){for(Pipe p:pipes)if(bird.getBounds().intersects(p.getTopBounds())||bird.getBounds().intersects(p.getBottomBounds()))return true;return false;}
    private void gameOver(){state=GameState.GAME_OVER;timer.stop();setWindowShortcutsEnabled(false);scores.record(score);notice="Enter a name, then save your score.";new SwingWorker<Void,Void>(){protected Void doInBackground(){scores.refreshBest();return null;}protected void done(){if(state==GameState.GAME_OVER)repaint();}}.execute();SwingUtilities.invokeLater(()->{nameField.setVisible(true);nameField.requestFocusInWindow();});}
    private void saveScore(){if(scoreSaved){notice="This game's score is already saved.";repaint();return;}if(scoreSaving)return;name=nameField.getText().trim();if(!name.matches("[A-Za-z0-9 _-]{1,24}")){notice="Use 1-24 letters, numbers, spaces, _ or -.";repaint();return;}final String playerName=name;final int submittedScore=score;scoreSaving=true;nameField.setEnabled(false);notice="Saving score...";repaint();new SwingWorker<List<ScoreManager.DatabaseScore>,Void>(){private ScoreManager.SaveResult result;protected List<ScoreManager.DatabaseScore> doInBackground(){result=scores.saveScore(playerName,submittedScore);return result==ScoreManager.SaveResult.SAVED?scores.getTopScores(10):Collections.emptyList();}protected void done(){scoreSaving=false;nameField.setEnabled(true);try{if(result==ScoreManager.SaveResult.SAVED){scoreSaved=true;leaders=get();nameField.setVisible(false);String error=scores.getLastError();notice=error.isEmpty()?"Score saved!":"Score saved, but the leaderboard could not be refreshed.";}else{String error=scores.getLastError();notice=error.isEmpty()?"Database unavailable; your local best is still kept.":error;}}catch(Exception e){notice=result==ScoreManager.SaveResult.SAVED?"Score saved, but the leaderboard could not be refreshed.":"Unable to save score.";System.err.println(notice+" "+e.getMessage());}repaint();}}.execute();}
    private void showBoard(){nameField.setVisible(false);timer.stop();leaders=Collections.emptyList();notice="Loading leaderboard...";state=GameState.LEADERBOARD;repaint();new SwingWorker<List<ScoreManager.DatabaseScore>,Void>(){protected List<ScoreManager.DatabaseScore> doInBackground(){return scores.getTopScores(10);}protected void done(){if(state!=GameState.LEADERBOARD)return;try{leaders=get();notice=scores.getLastError();}catch(Exception e){notice="Unable to load leaderboard.";System.err.println(notice+" "+e.getMessage());}repaint();}}.execute();}
    protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);drawWorld(g2);if(state==GameState.MENU)drawMenu(g2);else if(state==GameState.PLAYING)drawHud(g2);else if(state==GameState.PAUSED)drawPause(g2);else if(state==GameState.GAME_OVER)drawGameOver(g2);else drawBoard(g2);g2.dispose();}
    private void drawWorld(Graphics2D g){
        int horizon=365,theme=cityTheme();Color skyTop,skyBottom,groundTop,groundBottom;
        if(theme==NIGHT){skyTop=new Color(8,18,53);skyBottom=new Color(52,67,128);groundTop=new Color(47,59,104);groundBottom=new Color(13,25,55);}
        else if(theme==SUNSET){skyTop=new Color(113,61,123);skyBottom=new Color(252,156,91);groundTop=new Color(190,102,100);groundBottom=new Color(57,49,70);}
        else if(theme==RAIN){skyTop=new Color(53,72,99);skyBottom=new Color(128,151,166);groundTop=new Color(90,110,126);groundBottom=new Color(31,48,68);}
        else{skyTop=new Color(27,93,151);skyBottom=new Color(148,222,239);groundTop=new Color(183,239,226);groundBottom=new Color(31,110,94);}
        g.setPaint(new GradientPaint(0,0,skyTop,0,horizon,skyBottom));g.fillRect(0,0,W,horizon);
        g.setPaint(new GradientPaint(0,horizon,groundTop,0,H,groundBottom));g.fillRect(0,horizon,W,H-horizon);
        if(theme==NIGHT)drawNightSky(g);else if(theme==SUNSET)drawSunset(g);else if(theme==SUNLIGHT)drawSun(g,660,112);
        drawCloud(g,96-cloudOffset/8,94,theme);drawCloud(g,452-cloudOffset/11,154,theme);drawCloud(g,730-cloudOffset/15,70,theme);drawCity(g,horizon,theme);
        drawCityGround(g,horizon,theme);
        for(Pipe p:pipes)p.draw(g);
        if(state!=GameState.MENU&&state!=GameState.LEADERBOARD)bird.draw(g);
        if(theme==RAIN)drawRain(g);
    }
    private void drawSun(Graphics2D g,int x,int y){g.setColor(new Color(255,235,158,75));g.fillOval(x-48,y-48,132,132);g.setColor(new Color(255,247,198));g.fillOval(x-20,y-20,76,76);}
    private void drawSunset(Graphics2D g){g.setColor(new Color(255,172,82,75));g.fillOval(582,236,156,156);g.setColor(new Color(255,218,120));g.fillOval(610,263,100,100);}
    private void drawNightSky(Graphics2D g){g.setColor(new Color(220,235,255,180));for(int i=0;i<26;i++){int x=(i*97+cloudOffset/6)%W,y=24+(i*43)%235;g.fillOval(x,y,i%3==0?3:2,i%3==0?3:2);}g.setColor(new Color(235,243,255,80));g.fillOval(638,62,112,112);g.setColor(new Color(250,250,231));g.fillOval(656,79,76,76);g.setColor(new Color(207,218,230));g.fillOval(674,96,13,9);g.fillOval(704,117,16,11);}
    private void drawRain(Graphics2D g){
        g.setStroke(RAIN_DROP_STROKE);
        for(int i=0;i<120;i++){
            int x = (int)(((long)i * 71L + jungleOffset * 7L + (i % 13) * 19L) % (W + 140L)) - 60;
            int y = (int)(((long)i * 47L + cloudOffset * 13L) % (H + 120L)) - 80;
            int length = 12 + (i % 16);
            int drift = 6 + (i % 7);
            int alpha = 110 + (i % 9) * 18;
            g.setColor(RAIN_DROP_COLORS[(alpha-110)/18]);
            g.drawLine(x, y, x - drift, y + length);
            if((i % 5) == 0) {
                g.drawLine(x - 2, y + 4, x - drift - 4, y + length + 6);
            }
        }

        g.setStroke(RAIN_PUDDLE_STROKE);
        for(int i=0;i<18;i++){
            int x = (int)(((long)i * 89L + jungleOffset * 5L) % (W + 50L)) - 25;
            int y = GROUND_Y + 18 + ((i * 23) % 84);
            g.setColor(RAIN_PUDDLE_COLOR);
            g.fillOval(x, y, 18, 5);
            g.fillOval(x + 8, y + 3, 20, 6);
        }

        for(int i=0;i<5;i++){
            int x = 34 + i * 100 + (cloudOffset / 10 % 20);
            int y = GROUND_Y + 18 + (i % 3) * 9;
            g.setColor(RAIN_SPLASH_COLOR);
            g.fillOval(x, y, 44, 10);
        }
    }
    private void drawCity(Graphics2D g,int horizon,int theme){
        int far=jungleOffset/9%95, near=jungleOffset/3%124;
        Color farBody=theme==NIGHT?new Color(24,38,79):theme==SUNSET?new Color(103,56,91):theme==RAIN?new Color(61,83,105):new Color(71,113,149);
        Color nearBody=theme==NIGHT?new Color(12,23,63):theme==SUNSET?new Color(67,41,77):theme==RAIN?new Color(36,55,78):new Color(28,66,103);
        Color farWindows=theme==NIGHT?new Color(255,211,105,170):theme==SUNSET?new Color(255,191,120,145):theme==RAIN?new Color(183,211,226,100):new Color(180,230,244,105);
        Color nearWindows=theme==NIGHT?new Color(141,229,255,210):theme==SUNSET?new Color(255,221,166,190):theme==RAIN?new Color(155,194,214,145):new Color(122,211,238,155);
        for(int i=-2;i<11;i++){int type=Math.floorMod(i,4);drawBuilding(g,i*95-far,0,type,farBody,farWindows);}
        for(int i=-2;i<9;i++){int type=Math.floorMod(i+2,4);drawBuilding(g,i*124-near,1,type,nearBody,nearWindows);}
        g.setColor(theme==NIGHT?new Color(12,24,55):theme==SUNSET?new Color(57,43,65):theme==RAIN?new Color(28,44,63):new Color(20,49,72));g.fillRect(0,horizon-3,W,16);
    }
    private void drawBuilding(Graphics2D g,int x,int layer,int type,Color body,Color windows){
        if(x+112<0||x-4>W)return;
        int width=BUILDING_WIDTHS[type], bottom=365, top=layer==0?FAR_BUILDING_TOPS[type]:NEAR_BUILDING_TOPS[type];
        g.setColor(new Color(8,30,48,65));g.fillRect(x+8,top+9,width,bottom-top);
        g.setPaint(new GradientPaint(x,top,body.brighter(),x+width,top,body.darker()));g.fillRect(x,top,width,bottom-top);
        g.setColor(body.darker());g.fillPolygon(new int[]{x+width,x+width+11,x+width+11,x+width},new int[]{top,top+9,bottom+9,bottom},4);
        g.setColor(new Color(211,238,245,145));g.fillRect(x-4,top-7,width+8,9);
        if(type==1){g.setColor(body.darker());g.fillRect(x+width/2-3,top-30,6,30);g.setColor(new Color(224,246,247));g.fillOval(x+width/2-5,top-34,10,10);}
        if(type==2){g.setColor(new Color(153,223,242,110));g.fillRoundRect(x+8,top+8,width-16,bottom-top-16,8,8);}
        if(type==3){g.setColor(body.darker());g.fillRect(x+12,top-22,width-24,22);g.setColor(new Color(255,217,112));g.fillOval(x+width/2-5,top-17,10,10);}
        for(int wy=top+16;wy<bottom-14;wy+=22)for(int wx=x+12;wx<x+width-8;wx+=19){g.setColor(windows);g.fillRoundRect(wx,wy,type==2?width-24:9,12,2,2);if(type==2)break;}
    }
    private void drawCityGround(Graphics2D g,int horizon,int theme){
        Color top=theme==NIGHT?new Color(40,48,78):theme==SUNSET?new Color(94,68,82):theme==RAIN?new Color(57,75,94):new Color(73,102,111);Color bottom=theme==NIGHT?new Color(12,18,38):theme==SUNSET?new Color(34,34,52):theme==RAIN?new Color(20,35,52):new Color(25,42,55);g.setPaint(new GradientPaint(0,horizon,top,0,H,bottom));g.fillRect(0,horizon,W,H-horizon);
        int scroll=jungleOffset%92;
        // Discrete pavement slabs and soft reflections replace the former moving grid.
        for(int row=0;row<5;row++){int y=horizon+20+row*31;int h=18+row*3;for(int base=-92-scroll;base<W+92;base+=92){int x=base+(row%2)*36;g.setColor(new Color(157,194,198,28+row*8));g.fillRoundRect(x,y,58+row*7,h,8,8);g.setColor(new Color(214,247,241,22));g.fillOval(x+12,y+6,22,4);}}
        g.setColor(theme==NIGHT?new Color(53,72,109):theme==SUNSET?new Color(128,83,86):theme==RAIN?new Color(59,83,107):new Color(80,122,129));g.fillRoundRect(0,GROUND_Y-6,W,20,10,10);g.setColor(theme==NIGHT?new Color(14,25,50):theme==SUNSET?new Color(40,42,60):theme==RAIN?new Color(19,35,57):new Color(26,54,67));g.fillRect(0,GROUND_Y+12,W,H-GROUND_Y);
        for(int x=-80-scroll;x<W+100;x+=90){g.setColor(new Color(116,167,171,65));g.fillOval(x,GROUND_Y+27,38,6);}
    }
    private void drawCloud(Graphics2D g,int x,int y,int theme){x=((x%900)+900)%900-80;Color cloud=theme==NIGHT?new Color(136,151,198,75):theme==SUNSET?new Color(142,90,119,130):theme==RAIN?new Color(58,76,101,175):new Color(255,255,255,165);g.setColor(cloud);g.fillOval(x,y+15,58,28);g.fillOval(x+25,y,62,43);g.fillOval(x+65,y+16,52,27);g.fillRoundRect(x+21,y+22,70,22,18,18);}
    private void title(Graphics2D g,String s,int y,int size){g.setFont(new Font("SansSerif",Font.BOLD,size));g.setColor(Color.WHITE);FontMetrics f=g.getFontMetrics();g.drawString(s,(W-f.stringWidth(s))/2,y);}
    private void drawMenu(Graphics2D g){g.setColor(new Color(8,40,58,70));g.fillRoundRect(190,55,420,485,34,34);title(g,"FLAPPY BIRD",150,42);g.setFont(new Font("SansSerif",Font.PLAIN,18));g.setColor(new Color(231,250,255));String sub="Fly. Dodge. Survive.";g.drawString(sub,(W-g.getFontMetrics().stringWidth(sub))/2,182);Bird b=new Bird(377,230);b.draw(g);play.draw(g);how.draw(g);high.draw(g);if(!notice.isEmpty()){g.setFont(new Font("SansSerif",Font.PLAIN,14));g.setColor(Color.WHITE);g.drawString(notice,(W-g.getFontMetrics().stringWidth(notice))/2,560);}}
    private void drawHud(Graphics2D g){g.setColor(new Color(9,43,62,170));g.fillRoundRect(335,18,130,65,18,18);g.setColor(new Color(210,244,248));g.setFont(new Font("SansSerif",Font.BOLD,12));g.drawString("SCORE",376,39);g.drawString("LEVEL "+(difficultyLevel()+1),350,82);g.setFont(new Font("SansSerif",Font.BOLD,30));String s=""+score;g.drawString(s,400-g.getFontMetrics().stringWidth(s)/2,70);}
    private void overlay(Graphics2D g){g.setColor(new Color(3,18,34,165));g.fillRect(0,0,W,H);g.setColor(new Color(0,15,30,115));g.fillRoundRect(195,137,430,400,30,30);g.setPaint(new GradientPaint(185,125,new Color(17,68,91,240),185,525,new Color(7,33,53,242)));g.fillRoundRect(185,125,430,400,30,30);g.setColor(new Color(154,238,255,100));g.setStroke(new BasicStroke(2f));g.drawRoundRect(185,125,430,400,30,30);}
    private void drawPause(Graphics2D g){overlay(g);title(g,"PAUSED",215,34);resume.draw(g);restart.draw(g);menu.draw(g);}
    private void drawGameOver(Graphics2D g){overlay(g);title(g,"GAME OVER",200,34);g.setColor(new Color(213,242,245));g.setFont(new Font("SansSerif",Font.BOLD,13));g.drawString("SCORE",350,245);g.drawString("BEST",447,245);g.setFont(new Font("SansSerif",Font.BOLD,36));g.setColor(Color.WHITE);g.drawString(""+score,350,282);g.drawString(""+scores.getBest(),447,282);g.setFont(new Font("SansSerif",Font.PLAIN,15));g.setColor(Color.WHITE);g.drawString("PLAYER NAME",300,325);if(!nameField.isVisible()){g.setColor(new Color(255,255,255,35));g.fillRoundRect(300,337,200,38,10,10);g.setColor(Color.WHITE);g.drawString(name,313,362);}g.setFont(new Font("SansSerif",Font.PLAIN,13));g.setColor(new Color(215,236,242));g.drawString(notice,400-g.getFontMetrics().stringWidth(notice)/2,410);replay.draw(g);if(!scoreSaved)save.draw(g);board.draw(g);}
    private void drawBoard(Graphics2D g){g.setColor(new Color(7,41,59,210));g.fillRoundRect(150,55,500,500,30,30);title(g,"LEADERBOARD",120,31);g.setColor(new Color(178,225,230));g.setFont(new Font("SansSerif",Font.BOLD,14));g.drawString("RANK",195,160);g.drawString("PLAYER",310,160);g.drawString("SCORE",530,160);int y=195;if(leaders.isEmpty()){g.setFont(new Font("SansSerif",Font.PLAIN,16));g.setColor(Color.WHITE);String m=notice.isEmpty()?"No saved scores yet.":notice;g.drawString(m,(W-g.getFontMetrics().stringWidth(m))/2,245);}else for(int i=0;i<leaders.size();i++){ScoreManager.DatabaseScore e=leaders.get(i);g.setColor(new Color(255,255,255,22));g.fillRoundRect(180,y-22,440,34,10,10);g.setColor(Color.WHITE);g.drawString(""+(i+1),205,y);g.drawString(e.getPlayerName(),310,y);g.drawString(""+e.getScore(),550,y);y+=38;}back.draw(g);}
}
