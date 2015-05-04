import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public class DisplayServer extends JPanel implements KeyListener {

  private double _nextSpeed = 7.5;
  private double _nextOmega = 0;
  private double dSpeed = 0.1;
  private double maxOmega = Math.PI/4;

  private Simulator _sim;


  private static int historySkip = 5;
  private static final long serialVersionUID = 1l;

  protected double gvX [], gvY[], gvTheta[];
  protected int numVehicles = 0;
  protected int maxNumVehicles = 20;
  protected int shapeX[], shapeY[];
  protected JFrame frame;
  protected NumberFormat format = new DecimalFormat("#####.##");
  protected String myHostname;
  protected Color[] vehicleColors;
  protected Color[] pathColors;

  private final int SLEEP_TIME = 0; // delay between timesteps when drawing vehicle trajectories
  // 10-100 is a reasonable number

  // set display sizes
  private int minDisplayX = 500;
  private int minDisplayY = 500;
  private int preferredDisplayX = 500;
  private int preferredDisplayY = 500;


  /*
  protected Color[] my_colors = new Color[] {Color.black,Color.blue,Color.cyan,

					     Color.green, Color.magenta, 
					     Color.orange, Color.pink,
					     Color.red, Color.yellow,
					     Color.darkGray};
  */

  public class DisplayController extends VehicleController {

        private DisplayServer _ds;
//    private ControlPanel cp;

    private double minTransSpeed = 0;
    private double maxTransSpeed = 10;
    private double maxRotSpeed = Math.PI / 4;

    private double _startSpeed = 7.5;
    private double _startOmega = 0;

//    private double _nextSpeed = 7.5;
//    private double _nextOmega = 0;

    private double dSpeed = 0.5;
    private double maxOmega = Math.PI / 4;

    public DisplayController(Simulator s, GroundVehicle v) {
      super(s, v);

//        _ds = ds;

    }

    public void addDisplayServer(DisplayServer ds) {
      _ds = ds;
    }


    /**
     * Clamps speed and omega to allowable ranges and returns control with
     * bounded values of s and omega.
     *
     * @param s     forward speed
     * @param omega angular velocity
     * @return control with clamped linear and angular velocity values
     */
    private Control clampControl(double s, double omega) {

      double clampedSpeed;
      double clampedOmega;

      // clamp speed if it is above 10 or below 5
      if (s > maxTransSpeed)
        clampedSpeed = maxTransSpeed;
      else if (s < minTransSpeed)
        clampedSpeed = minTransSpeed;
      else
        clampedSpeed = s;

      // clamp angular velocity if it is above the allowed range
      clampedOmega = Math.min(Math.max(omega, -Math.PI / 4), Math.PI / 4);

      // create a control with the clamped s and omega values
      Control clampedControl = new Control(clampedSpeed, clampedOmega);
      return clampedControl;
    }


    public Control getControl(int sec, int msec) {
      double _nextSpeed = _ds.getUserSpeed();
      double _nextOmega = _ds.getUserOmega();
      System.out.println("s: " + _nextSpeed + " omega: " + _nextOmega);
      return clampControl(_nextSpeed, _nextOmega);
    }

  }

  public void addSimulator(Simulator sim) {
    _sim = sim;
  }

  public class History {
    History() {
      myX = new double[100000];
      myY = new double[100000];
      myNumPoints = 0;
      loopHistory = 0;
      trueHistoryLength = 0;
    }
    public double [] myX;
    public double [] myY;
    int myNumPoints;
    int trueHistoryLength;
    int loopHistory;
  }

  History [] histories;
  boolean trace = false;

  public synchronized void clear() {
    if (histories !=null){
      for (int i = 0; i < histories.length; i++) {
        histories[i].myNumPoints = 0;
        histories[i].loopHistory = 0;
        histories[i].trueHistoryLength = 0;
      }
    }
  }

  public synchronized void resetHistories(int numVehicles) {
    histories = new History[numVehicles];
    for (int i = 0; i < numVehicles; i++)
      histories[i] = new History();
  }


  public class MessageListener extends Thread {
    public BufferedReader my_client;
    public DisplayServer my_display;
    public MessageListener(Socket client, DisplayServer display) {
      my_display = display;
      try {
        //System.out.println("Default size: " + client.getReceiveBufferSize());
        my_client = new BufferedReader
                (new InputStreamReader(client.getInputStream()));
      }
      catch (IOException e) {
        System.err.println("Very weird IOException in creating the BufferedReader");
        System.err.println(e);
        System.exit(-1);
      }
    }
    public void run() {
      try {
        while (true) {
          String message = my_client.readLine();
          if (message == null){
            System.out.println("EOF reached!");
            return; //EOF reached
          }

          StringTokenizer st = new StringTokenizer(message);
          //System.out.println("Received: " + message);
          String tok = st.nextToken();
          if (tok.equals("clear")) {
            my_display.clear();
          }
          else if (tok.equals("traceon")) {
            synchronized (my_display) {
              my_display.trace = true;
            }
          } else if (tok.equals("traceoff")) {
            synchronized (my_display) {
              my_display.trace = false;
            }
          } else if (tok.equals("close")){
            return;
          } else {
            synchronized (my_display) {
              if (my_display.numVehicles != Integer.parseInt(tok)) {
                my_display.numVehicles = Integer.parseInt(tok);
                my_display.gvX = new double[my_display.numVehicles];
                my_display.gvY = new double[my_display.numVehicles];
                my_display.gvTheta = new double[my_display.numVehicles];
                my_display.resetHistories(numVehicles);
              }
              for (int i = 0; i < my_display.numVehicles; i++) {
                tok = st.nextToken();
                my_display.gvX[i] = Double.parseDouble(tok);
                tok = st.nextToken();
                my_display.gvY[i] = Double.parseDouble(tok);
                tok = st.nextToken();
                my_display.gvTheta[i] = Double.parseDouble(tok);
                if (trace) {
                  if (histories[i].trueHistoryLength % historySkip == 0){


                    int n;
                    if (histories[i].myNumPoints == histories[i].myX.length) {
                      n = 0;
                      histories[i].myNumPoints = 0;
                      histories[i].loopHistory = 1;
                    } else {
                      n = histories[i].myNumPoints;
                      histories[i].myNumPoints++;
                    }
                    histories[i].myX[n] = my_display.gvX[i];
                    histories[i].myY[n] = my_display.gvY[i];
                  }
                  histories[i].trueHistoryLength++;
                } // end if (trace)
              } // end for (int i = 0; i < my_display.numVehicles; i++)
            } // End synchronized (my_display)
          }
          my_display.repaint();
          try {
            sleep(SLEEP_TIME);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      catch (IOException e) {
      }
      return;
    }
  }

  public DisplayServer (String hostname) {
    myHostname = hostname;
    shapeX = new int[9];
    shapeY = new int[9];

    // This is just the UAV shape centred at the origin.
    // If you wanted to draw a more realistic UAV, you would modify this
    // polygon. 

    shapeX[0] = 10;  shapeY[0] = 0;
    shapeX[1] = 0;   shapeY[1] = -5;
    shapeX[2] = 0;   shapeY[2] = -2;
    shapeX[3] = -8;  shapeY[3] = -2;
    shapeX[4] = -10; shapeY[4] = -4;
    shapeX[5] = -10; shapeY[5] = 4;
    shapeX[6] = -8;  shapeY[6] = 2;
    shapeX[7] = 0;   shapeY[7] = 2;
    shapeX[8] = 0;   shapeY[8] = 5;

    // generate array of random colors for up to 20 different vehicles
    vehicleColors = new Color[maxNumVehicles];
    pathColors = new Color[maxNumVehicles];


    // preset colors
    vehicleColors[0] = new Color(255,21,60); // red
    pathColors[0] = new Color(255,21+50,60+50);
    vehicleColors[1] = new Color(69,127,255); // blue
    pathColors[1] = new Color(69+50,127+50,255);
    vehicleColors[2] = new Color(232,117,31); // orange
    pathColors[2] = new Color(255,117+50,31+50);
    vehicleColors[3] = new Color(255,209,21); // yellow
    pathColors[3] = new Color(255,255,21+50);
    vehicleColors[4] = new Color(160,67,232); // purple
    pathColors[4] = new Color(160+50,67+50,255);


    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        startGraphics();
      }
    });
  }

  public void startGraphics()
  {
    JFrame.setDefaultLookAndFeelDecorated(true);

    frame = new JFrame("16.35 Display");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Container container = frame.getContentPane();
    //container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
    container.setLayout(new BorderLayout());

    setOpaque(true);
    setFocusable(true);
    setMinimumSize(new Dimension(minDisplayX, minDisplayY));
    setPreferredSize(new Dimension(preferredDisplayX, preferredDisplayY));
    addKeyListener(this);
    container.add(this,BorderLayout.WEST);
    setVisible(true);

    frame.pack();
    frame.setVisible(true);
  }



  public void keyPressed(KeyEvent e) {
    int code = e.getKeyCode();
    _nextOmega = 0;
    {
      if (code == KeyEvent.VK_DOWN) {
        _nextSpeed -= dSpeed;
        System.out.println("DOWN");
      }
      if (code == KeyEvent.VK_UP) {
        _nextSpeed += dSpeed;
        System.out.println("UP");
      }
      if (code == KeyEvent.VK_LEFT) {
        _nextOmega = maxOmega;
        System.out.println("LEFT");

      }
      if (code == KeyEvent.VK_RIGHT) {
        _nextOmega = -maxOmega;
        System.out.println("RIGHT");

      }
    }
  }

  public double getUserSpeed() {
    System.out.println("getUserSpeed() = "+_nextSpeed);
    return _nextSpeed;
  }

  public double getUserOmega() {
    return _nextOmega;
  }

  public void keyReleased(KeyEvent e) { }

  public void keyTyped(KeyEvent e)
  {
    switch (e.getKeyChar()) {
      case 'q':
      case 'Q':
        System.exit(0);
    }
    int code = e.getKeyCode();

//    {
//      if (code == KeyEvent.VK_DOWN) {
//        _nextSpeed -= dSpeed;
//        System.out.println("DOWN");
//      }
//      if (code == KeyEvent.VK_UP) {
//        _nextSpeed += dSpeed;
//        System.out.println("UP");
//      }
//      if (code == KeyEvent.VK_LEFT) {
//        _nextOmega -= dOmega;
//        System.out.println("LEFT");
//
//      }
//      if (code == KeyEvent.VK_RIGHT) {
//        _nextOmega += dOmega;
//        System.out.println("RIGHT");
//
//      }
//    }
  }


  /**
   *
   * @return array with random RGB color pair (dark color and light color)
   */
  public static Color[] randomColorPair() {
    Random rand = new Random();
    int r = rand.nextInt(155); // 255 will result in colors that can't be detected on a white background
    int g = rand.nextInt(155);
    int b = rand.nextInt(155);
    Color darkColor = new Color(r,g,b);
    Color lightColor = new Color(r+100,g+100,b+100);
    Color[] colorPair = new Color[2];
    colorPair[0] = darkColor;
    colorPair[1] = lightColor;
    return colorPair;
  }

  /**
   *
   * @return random dark RGB color
   */
  public static Color randomDarkColor() {
    Random rand = new Random();
    int r = rand.nextInt(155); // 255 will result in colors that can't be detected on a white background
    int g = rand.nextInt(155);
    int b = rand.nextInt(155);
    return new Color(r,g,b);
  }

  /**
   *
   * @return random light RGB color
   */
  public static Color randomLightColor() {
    Random rand = new Random();
    int r = rand.nextInt(205) + 50;
    int g = rand.nextInt(205) + 50;
    int b = rand.nextInt(155) + 100;
    return new Color(r, g, b);
  }

  protected synchronized void drawVehicles(Graphics g) {
    g.setColor(Color.black);

//
//    // generate random colors
//    for (int i=0; i<numVehicles; i++) {
//      Color[] colorPair = randomColorPair();
//      vehicleColors[i] = colorPair[0];
//      pathColors[i] = colorPair[1];
//    }

    // This chunk of code just translate and rotates the shape.

    for (int j = 0; j < numVehicles; j++) {
      if (j < vehicleColors.length){
        g.setColor(vehicleColors[j]);

      }else{
        g.setColor(vehicleColors[vehicleColors.length-1]);
      }
      int drawX[] = new int[9];
      int drawY[] = new int[9];

      for (int i = 0; i < 9; i++) {
        // We scale the x and y by 5, since the bounds on X and Y are 100x100
        // but our windows is 500x500.

        double x = gvX[j]*5;
        double y = gvY[j]*5;
        double th = gvTheta[j];
        drawX[i] = (int)(x+Math.cos(th)*shapeX[i]+Math.sin(th)*shapeY[i]);
        drawY[i] = (int)(y+Math.sin( th)*shapeX[i]-Math.cos(th)*shapeY[i]);
        drawY[i] = 500- drawY[i];
      }
      g.drawPolygon(drawX, drawY, 9);
    }
  }

  protected synchronized void drawHistories(Graphics g) {
    g.setColor(Color.black);

    // This chunk of code just translate and rotates the shape.

    for (int j = 0; j < numVehicles; j++) {
      if (j < pathColors.length){
        g.setColor(pathColors[j]);

      }else{
        g.setColor(pathColors[pathColors.length-1]);
      }
      int drawX[]; int drawY[];
      if (histories[j].loopHistory == 0){
        drawX = new int[histories[j].myNumPoints];
        drawY = new int[histories[j].myNumPoints];
      }
      else{

        drawX = new int[histories[j].myX.length];
        drawY = new int[histories[j].myY.length];
      }
      for (int i = 0; i < drawX.length;i++){
        // We scale the x and y by 5, since the bounds on X and Y are 100x100
        // but our windows is 500x500.

        double x = histories[j].myX[i]*5;
        double y = histories[j].myY[i]*5;
        drawX[i] = (int)(x);
        drawY[i] = 500- (int)y;
      }
      g.drawPolygon(drawX, drawY, drawX.length);
    }
  }

  /**
   * Draws circle based on x, y, and radius given in simulation coordinate system.
   * @param g   graphics object
   * @param Xc  x location of circle center
   * @param Yc  y location of circle center
   * @param R   circle radius
   */
  public static void drawCircle(Graphics g, int Xc, int Yc, int R) {

    int diameter = 2*R;

    // shift x and y by circle radius to center it
    g.fillOval((Xc-R)*5, (Yc-R)*5, diameter*5, diameter*5);

  }

  protected void paintComponent(Graphics g) {
    super.paintComponent(g); //paints the background and image

    Rectangle bounds = this.getBounds();
    g.setColor(Color.white);
    g.fillRect(0, 0, bounds.width, bounds.height);

    // draw circle for ideal CircleController trajectory
    g.setColor(randomLightColor());
    drawCircle(g,50,50,25);

    g.setColor(Color.black);
    g.setColor(randomLightColor());
    g.drawString("Display running in the 90's", 10, 12);
    g.drawString("on "+myHostname, 10,25);
    if (trace)
      drawHistories(g);
    drawVehicles(g);
  }

  protected void addClient(Socket client) {
    MessageListener l = new MessageListener(client, this);
    l.start();
  }
/*
  public static void main(String [] argv) {
    double[] pos = {10, 10, 0};

    // construct a single GroundVehicle
//            GroundVehicle gv = new GroundVehicle(
//                    Simulator.randomStartingPosition(),
//                    Simulator.randomDoubleInRange(0, 10),
//                    Simulator.randomDoubleInRange(-Math.PI / 4, Math.PI / 4));

    GroundVehicle gv = new GroundVehicle(pos, 1, 0);


    Thread gvThread = new Thread(gv);

    try {
      ServerSocket s = new ServerSocket(5065);
      s.setReuseAddress(true);
      if (!s.isBound()) {
        System.exit(-1);
      }
      String address = GeneralInetAddress.getLocalHost().getHostAddress();

      DisplayServer d = new DisplayServer(address);
      // create DisplayClient
      DisplayClient dc = new DisplayClient(address);

      // construct a single Simulator
      Simulator sim = new Simulator(dc);
      sim.addVehicle(gv);
      Thread simThread = new Thread(sim);

      // construct a single instance of the CircleController class
      UserController uc = new UserController(sim,gv);
      uc.addDisplayServer(dc);
      Thread ucThread = new Thread(uc);

      gvThread.start();
      ucThread.start();
      simThread.start();

      do {
        Socket client = s.accept();
        d.addClient(client);
      } while (true);
    }
    catch (IOException e) {
      System.err.println("I couldn't create a new socket.\n"+
              "You probably are already running DisplayServer.\n");
      System.err.println(e);
      System.exit(-1);
    }



  }
  */

}