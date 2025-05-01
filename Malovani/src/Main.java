// Importy potřebné pro GUI, kreslení a práci s událostmi
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class Main extends JPanel {
    // Definice režimů kreslení
    private enum Mode { PEN, ERASER, LINE, RECT, SQUARE, CIRCLE, POLYGON }

    // Plátno pro kreslení
    private BufferedImage canvas;

    // Nastavení kreslení
    private int currentThickness = 3;
    private int currentStyle = 0; // 0 = plná, 1 = přerušovaná, 2 = tečkovaná
    private Color currentColor = Color.BLACK;
    private Color previousColor = Color.BLACK;
    private boolean eraserMode = false;
    private Mode currentMode = Mode.PEN;

    // Pomocné body pro kreslení tvarů
    private Point startPoint = null;
    private Point previewPoint = null;
    private int polygonSides = 5;

    public Main(int width, int height) {
        // Inicializace plátna a jeho vyčištění
        canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        clearCanvas();

        // Ovládání myši – kreslení a interakce
        MouseAdapter adapter = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                if (currentMode == Mode.ERASER) toggleEraser();
            }

            @Override public void mouseDragged(MouseEvent e) {
                // Kreslení perem nebo gumou
                if (currentMode == Mode.PEN || currentMode == Mode.ERASER) {
                    Point p = e.getPoint();
                    drawLineRaster(canvas, startPoint.x, startPoint.y, p.x, p.y);
                    startPoint = p;
                    repaint();
                } else {
                    // U ostatních módů jen náhled
                    previewPoint = e.getPoint();
                    repaint();
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (currentMode != Mode.PEN && currentMode != Mode.ERASER) {
                    // Po uvolnění myši vykreslíme tvar
                    Point end = e.getPoint();
                    switch (currentMode) {
                        case LINE:    drawLineRaster(canvas, startPoint.x, startPoint.y, end.x, end.y); break;
                        case RECT:    drawRectangleRaster(canvas, startPoint, end); break;
                        case SQUARE:  drawSquareRaster(canvas, startPoint, end); break;
                        case CIRCLE:  drawCircleRaster(canvas, startPoint, end); break;
                        case POLYGON: drawPolygonRaster(canvas, startPoint, end, polygonSides); break;
                    }
                    previewPoint = null;
                    startPoint = null;
                    repaint();
                }
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    // Bresenhamův algoritmus pro vykreslení úsečky s tloušťkou a stylem
    private void drawLineRaster(BufferedImage img, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy, e2, count = 0;
        while (true) {
            if (shouldDrawPixel(count)) drawPixelBlock(img, x0, y0);
            if (x0 == x1 && y0 == y1) break;
            e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx)  { err += dx; y0 += sy; }
            count++;
        }
    }

    // Určuje, zda pixel vykreslit (podle stylu čáry)
    private boolean shouldDrawPixel(int count) {
        switch (currentStyle) {
            case 1: return (count / 10) % 2 == 0; // přerušovaná
            case 2: return (count % 10) < 2; // tečkovaná
            default: return true; // plná
        }
    }

    // Vykreslí čtverec pixelů podle tloušťky
    private void drawPixelBlock(BufferedImage img, int x, int y) {
        for (int i = -currentThickness/2; i <= currentThickness/2; i++)
            for (int j = -currentThickness/2; j <= currentThickness/2; j++) {
                int px = x + i, py = y + j;
                if (px>=0 && py>=0 && px<img.getWidth() && py<img.getHeight())
                    img.setRGB(px, py, currentColor.getRGB());
            }
    }

    // Vykreslení obdélníku pomocí čtyř úseček
    private void drawRectangleRaster(BufferedImage img, Point p0, Point p1) {
        drawLineRaster(img, p0.x, p0.y, p1.x, p0.y);
        drawLineRaster(img, p1.x, p0.y, p1.x, p1.y);
        drawLineRaster(img, p1.x, p1.y, p0.x, p1.y);
        drawLineRaster(img, p0.x, p1.y, p0.x, p0.y);
    }

    // Vykreslení čtverce – vypočte nejdelší stranu a vytvoří obdélník se stejnou délkou stran
    private void drawSquareRaster(BufferedImage img, Point p0, Point p1) {
        int dx = p1.x - p0.x, dy = p1.y - p0.y;
        int side = Math.max(Math.abs(dx), Math.abs(dy));
        int sx = dx==0?1:(dx>0?1:-1);
        int sy = dy==0?1:(dy>0?1:-1);
        Point p3 = new Point(p0.x + sx*side, p0.y + sy*side);
        drawRectangleRaster(img, p0, p3);
    }

    // Vykreslení kružnice jako mnohoúhelníku s malým krokem
    private void drawCircleRaster(BufferedImage img, Point c, Point e) {
        int r = (int)c.distance(e), steps = r*8;
        Point prev = null;
        for (int i=0; i<=steps; i++) {
            double a = 2*Math.PI*i/steps;
            int x = c.x + (int)(r*Math.cos(a));
            int y = c.y + (int)(r*Math.sin(a));
            if (prev!=null) drawLineRaster(img, prev.x, prev.y, x, y);
            prev = new Point(x,y);
        }
    }

    // Vykreslení pravidelného n-úhelníku
    private void drawPolygonRaster(BufferedImage img, Point c, Point e, int sides) {
        int r = (int)c.distance(e);
        Point first=null, prev=null;
        for (int i=0;i<sides;i++){
            double a=2*Math.PI*i/sides;
            int x=c.x+(int)(r*Math.cos(a));
            int y=c.y+(int)(r*Math.sin(a));
            if(prev!=null) drawLineRaster(img, prev.x, prev.y, x, y);
            else first=new Point(x,y);
            prev=new Point(x,y);
        }
        if(first!=null&&prev!=null) drawLineRaster(img, prev.x, prev.y, first.x, first.y);
    }

    // Vyčistí celé plátno na bílo
    public void clearCanvas() {
        for(int x=0;x<canvas.getWidth();x++)
            for(int y=0;y<canvas.getHeight();y++)
                canvas.setRGB(x,y,Color.WHITE.getRGB());
        repaint();
    }

    // Aktivace režimu kreslení
    public void activateMode(Mode m) {
        currentMode = m;
        if (m != Mode.ERASER && eraserMode) toggleEraser();
        if (m == Mode.POLYGON) {
            String s=JOptionPane.showInputDialog("Počet stran:",polygonSides);
            if(s!=null) try{polygonSides=Integer.parseInt(s);}catch(Exception ignored){}
        }
        startPoint=null; previewPoint=null;
    }

    // Přepnutí režimu gumy – přepíná mezi aktuální barvou a bílou
    public void toggleEraser() {
        if(!eraserMode){previousColor=currentColor; currentColor=Color.WHITE;}
        else           {currentColor=previousColor;}
        eraserMode=!eraserMode;
    }

    // Vykreslení komponenty – plátna a případného náhledu tvaru
    @Override protected void paintComponent(Graphics gBase) {
        super.paintComponent(gBase);
        gBase.drawImage(canvas,0,0,null);

        // Náhled tvaru (při tažení myši)
        if(startPoint!=null && previewPoint!=null && currentMode!=Mode.PEN && currentMode!=Mode.ERASER) {
            BufferedImage temp=new BufferedImage(canvas.getWidth(),canvas.getHeight(),canvas.getType());
            for(int x=0;x<canvas.getWidth();x++)
                for(int y=0;y<canvas.getHeight();y++)
                    temp.setRGB(x,y,canvas.getRGB(x,y));
            switch(currentMode) {
                case LINE:    drawLineRaster(temp,startPoint.x,startPoint.y,previewPoint.x,previewPoint.y); break;
                case RECT:    drawRectangleRaster(temp,startPoint,previewPoint); break;
                case SQUARE:  drawSquareRaster(temp,startPoint,previewPoint); break;
                case CIRCLE:  drawCircleRaster(temp,startPoint,previewPoint); break;
                case POLYGON: drawPolygonRaster(temp,startPoint,previewPoint,polygonSides); break;
            }
            gBase.drawImage(temp,0,0,null);
        }
    }

    // Hlavní metoda – vytvoření okna a přidání ovládacích prvků
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Rasterové malování");
            Main panel = new Main(888, 600);
            JPanel top = new JPanel();

            // Tlačítka pro výběr režimu
            JButton pen = new JButton("Pero");
            pen.addActionListener(e -> panel.activateMode(Mode.PEN));
            top.add(pen);

            JButton eraser = new JButton("Guma");
            eraser.addActionListener(e -> panel.activateMode(Mode.ERASER));
            top.add(eraser);

            // Tlačítko s výběrem tvarů
            JButton shapes = new JButton("Tvary");
            JPopupMenu shapeMenu = new JPopupMenu();
            JMenuItem li = new JMenuItem("Přímka");    li.addActionListener(e -> panel.activateMode(Mode.LINE));    shapeMenu.add(li);
            JMenuItem re = new JMenuItem("Obdélník");  re.addActionListener(e -> panel.activateMode(Mode.RECT));    shapeMenu.add(re);
            JMenuItem sq = new JMenuItem("Čtverec");   sq.addActionListener(e -> panel.activateMode(Mode.SQUARE));  shapeMenu.add(sq);
            JMenuItem ci = new JMenuItem("Kružnice");  ci.addActionListener(e -> panel.activateMode(Mode.CIRCLE));  shapeMenu.add(ci);
            JMenuItem po = new JMenuItem("Polygon");   po.addActionListener(e -> panel.activateMode(Mode.POLYGON)); shapeMenu.add(po);
            shapes.addActionListener(e -> shapeMenu.show(shapes, 0, shapes.getHeight()));
            top.add(shapes);

            // Výběr barvy
            JButton color = new JButton("Barva");
            color.addActionListener(e -> {
                JPopupMenu colorMenu = new JPopupMenu();
                JMenuItem black = new JMenuItem("Černá");  black.addActionListener(ev -> panel.currentColor = Color.BLACK); colorMenu.add(black);
                JMenuItem white = new JMenuItem("Bílá");   white.addActionListener(ev -> panel.currentColor = Color.WHITE); colorMenu.add(white);
                JMenuItem red   = new JMenuItem("Červená");red.addActionListener(ev -> panel.currentColor = Color.RED);   colorMenu.add(red);
                JMenuItem green = new JMenuItem("Zelená"); green.addActionListener(ev -> panel.currentColor = Color.GREEN); colorMenu.add(green);
                JMenuItem blue  = new JMenuItem("Modrá");  blue.addActionListener(ev -> panel.currentColor = Color.BLUE);  colorMenu.add(blue);
                colorMenu.show(color, 0, color.getHeight());
            });
            top.add(color);

            // Nastavení tloušťky
            JButton thickness = new JButton("Tloušťka");
            thickness.addActionListener(e -> {
                String s = JOptionPane.showInputDialog("Zadej tloušťku:", panel.currentThickness);
                if (s != null) try { panel.currentThickness = Integer.parseInt(s); } catch (Exception ignored) {}
            });
            top.add(thickness);

            // Styl čáry
            JButton style = new JButton("Styl");
            style.addActionListener(e -> {
                String[] opts = {"Plná", "Přerušovaná", "Tečkovaná"};
                int ch = JOptionPane.showOptionDialog(panel, "Vyber styl", "Styl", JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);
                if (ch >= 0) panel.currentStyle = ch;
            });
            top.add(style);

            // Tlačítko pro vymazání celé plochy
            JButton clear = new JButton("Vymazat Vše");
            clear.addActionListener(e -> panel.clearCanvas());
            top.add(clear);

            // Nastavení hlavního okna
            frame.setLayout(new BorderLayout());
            frame.add(top, BorderLayout.NORTH);
            frame.add(panel, BorderLayout.CENTER);
            frame.setSize(900, 700);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}
