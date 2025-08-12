import Journeymap.Journeymap;
import Journeymap.Waypoint;
import Journeymap.MapType;
import Journeymap.World;
import Journeymap.Dimension;
import Journeymap.PointXZ;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.IntFunction;

public class JourneymapViewer extends JPanel {
    private static final int TOOLBAR_HEIGHT = 30;
    private static final int REGION_SIZE = 512;

    private static final Color TOOLBAR_COLOR = new Color(50, 50, 50);
    private static final Color BACKGROUND_COLOR = new Color(25, 25, 25);

    private static String minecraftDirectory;
    private static Journeymap journeymap;
    private String selectedMode;

    static {
        try {
            journeymap = new Journeymap(
                    "/Users/ashton/Library/Application Support/ModrinthApp/profiles/1.21.4 fabric/journeymap",
                    new World("wild~survival", true)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    ;
    private static World world;
    private Dimension currentDimension;
    private MapType currentMapType;
    private int caveLayer = 23;

    private int panX = 0;
    private int panY = 0;
    private int panSpeed = 16;
    private float zoom = 1.0f;
    private int[] viewableRectangle;
    private final HashMap<PointXZ, BufferedImage> cachedRegions;
    private final HashMap<Waypoint, BufferedImage> cachedWaypoints;

    public JourneymapViewer() {
        setBackground(BACKGROUND_COLOR);
        setFocusable(true);

        cachedRegions = new HashMap<>();
        cachedWaypoints = new HashMap<>();
        currentDimension = Dimension.OVERWORLD;
        currentMapType = MapType.DAY;
    }

    public void setZoom(float value) {
        if (value <= 0.0f) throw new IllegalArgumentException("Zoom must be > 0");
        zoom = value;
    }

    public Point getCenter() {
        return new Point(
                getWidth() / 2,
                getHeight() / 2
        );
    }

    public void setViewableRectangle() {
        viewableRectangle = new int[] {
                panX,
                panY,
                panX + getWidth(),
                panY + getHeight()
        };
    }

    public Point getViewableCenter() {
        return new Point(
                (viewableRectangle[0] + viewableRectangle[2]) / 2,
                (viewableRectangle[1] + viewableRectangle[3]) / 2
        );
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        setViewableRectangle();
        drawRegions(g);
//        drawWaypoints(g);

        System.out.println(Arrays.toString(viewableRectangle));
    }

    public void zoomAt(float zoomFactor, Point focus) {
        if (zoomFactor <= 0.0f) return;

        // Convert screen focus point to world coords
        float worldX = (focus.x + panX) / zoom;
        float worldY = (focus.y + panY) / zoom;

        // Apply new zoom
        zoom *= zoomFactor;

        // Recalculate pan so the world point stays at the same screen position
        panX = (int) (worldX * zoom - focus.x);
        panY = (int) (worldY * zoom - focus.y);

        repaint();
    }

    public void drawRegions(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int regionTileSize = (int)(REGION_SIZE * zoom);

        Point viewableTopLeft = new Point(
                viewableRectangle[0] / regionTileSize - 1,
                viewableRectangle[1] / regionTileSize - 1
        );
        Point viewableBottomRight = new Point(
                viewableRectangle[2] / regionTileSize + 1,
                viewableRectangle[3] / regionTileSize + 1
        );
        // System.out.printf("%d, %d\n", viewableBottomRight.x-viewableTopLeft.x, viewableBottomRight.y-viewableTopLeft.y);

        HashSet<PointXZ> usedRegions = new HashSet<>();
        for (int regionZ = viewableTopLeft.y; regionZ < viewableBottomRight.y; regionZ++) {
            for (int regionX = viewableTopLeft.x; regionX < viewableBottomRight.x; regionX++) {
                PointXZ regionXZ = new PointXZ(regionX, regionZ);
                BufferedImage region;

                if (cachedRegions.containsKey(regionXZ)) {
                    region = cachedRegions.get(regionXZ);
                } else {
                    try {
                        if (currentMapType == MapType.CAVE) region = journeymap.getCaveRegion(currentDimension, 0, regionXZ);
                        else region = journeymap.getRegion(currentDimension, currentMapType, regionXZ);
                        cachedRegions.put(regionXZ, region);
                    } catch (IOException e) {
                        continue;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                int drawX = regionX * regionTileSize - viewableRectangle[0];
                int drawY = regionZ * regionTileSize - viewableRectangle[1];

                g2.drawImage(
                        region,
                        drawX, drawY,
                        drawX + regionTileSize, drawY + regionTileSize, // destination size
                        0, 0,
                        REGION_SIZE, REGION_SIZE, // source size
                        null
                );

                usedRegions.add(regionXZ);
                // System.out.printf("Draw %d,%d at screen %d,%d\n", regionX, regionZ, drawX, drawY);
            }
        }

        // clear cache
        cachedRegions.keySet().removeIf(k -> !usedRegions.contains(k));
    }

    public void drawWaypoints(Graphics g) {
        List<String> usedWaypoints = new ArrayList<>();
        for (String guid : journeymap.getWaypoints().keySet()) {
            Waypoint wp = journeymap.getWaypoints().get(guid);

            BufferedImage icon;
            try {
                icon = cachedWaypoints.containsKey(wp) ? cachedWaypoints.get(wp) : wp.icon().render();
            } catch (IOException e) {
                System.out.println(e);
                continue;
            }

            int drawX = -(int)((wp.position().x + viewableRectangle[0]) * zoom);
            int drawY = -(int)((wp.position().y + viewableRectangle[1]) * zoom);

            if (!(-wp.icon().getWidth() < drawX && drawX < getWidth()) || !(-wp.icon().getHeight() < drawY && drawY < getHeight()))
                continue;
            System.out.printf("drawing waypoint %s at (%d, %d)\n", wp.name(), drawX, drawY);

            g.drawImage(icon, drawX, drawY, null);

            // clear cache
            usedWaypoints.add(guid);
        }

        cachedWaypoints.keySet().removeIf(k -> !usedWaypoints.contains(k.guid()));
    }

    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame("Journeymap Viewer");
        frame.setIconImage(ImageIO.read(new File("assets/icon.png")));
        frame.setSize(800, 500);
        frame.setMinimumSize(new java.awt.Dimension(800, 500));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // --- Toolbar ---
        JPanel toolbar = new JPanel();
        toolbar.setBackground(TOOLBAR_COLOR);
        toolbar.setPreferredSize(new java.awt.Dimension(frame.getWidth(), TOOLBAR_HEIGHT));
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
        frame.add(toolbar, BorderLayout.NORTH);

        JButton minecraftSelectButton = new JButton("select minecraft");
        toolbar.add(minecraftSelectButton);

        JButton worldSelectButton = new JButton("select world");
        toolbar.add(worldSelectButton);

        JMenu mapTypeMenu = new JMenu("Overworld");
        toolbar.add(mapTypeMenu);
        mapTypeMenu.setVisible(false);

        // --- Map Canvas ---
        JourneymapViewer canvas = new JourneymapViewer();
        frame.add(canvas, BorderLayout.CENTER);

        // actions
        minecraftSelectButton.addActionListener(e -> {
            JFileChooser minecraftSelector = getMinecraftSelector();
            int result = minecraftSelector.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedDir = minecraftSelector.getSelectedFile();
                minecraftDirectory = selectedDir.getAbsolutePath();
                System.out.println("Selected folder: " + minecraftDirectory);
                canvas.repaint();
            } else {
                System.out.println("File picker canceled");
            }

            if (journeymap != null) {
                mapTypeMenu.removeAll();
                for (String d : journeymap.getDimensions()) {
                    mapTypeMenu.add(new JMenuItem(d));
                }
            }
        });

        worldSelectButton.addActionListener(e -> {
            frame.getContentPane().add(canvas.getWorldSelector(frame, canvas), BorderLayout.CENTER);
            frame.revalidate();
            canvas.requestFocusInWindow();
        });

        canvas.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
//                System.out.println(key);

                switch (key) {
                    case 38: // up
                        canvas.panY -= canvas.panSpeed;
                        canvas.repaint();
                        break;
                    case 40: // down
                        canvas.panY += canvas.panSpeed;
                        canvas.repaint();
                        break;
                    case 37: // left
                        canvas.panX -= canvas.panSpeed;
                        canvas.repaint();
                        break;
                    case 39: // right
                        canvas.panX += canvas.panSpeed;
                        canvas.repaint();
                        break;
                    case 61: // zoom in
                        if (canvas.zoom > 4) break;
                        canvas.zoomAt(1.1f, canvas.getCenter());
                        break;
                    case 45: // zoom out
                        if (canvas.zoom < 0.1) break;
                        canvas.zoomAt(0.9f, canvas.getCenter());
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        frame.setVisible(true);
        canvas.requestFocus();
    }

    private static JFileChooser getMinecraftSelector() {
        JFileChooser minecraftSelector = new JFileChooser();
        minecraftSelector.setDialogTitle("Select your folder");
        minecraftSelector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        minecraftSelector.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() && f.getName().equals("journeymap");
            }

            @Override
            public String getDescription() {
                return "";
            }
        });
        minecraftSelector.setAcceptAllFileFilterUsed(false);
        return minecraftSelector;
    }

    private JPanel getWorldSelector(JFrame frame, JPanel panel) {
        JPanel worldSelector = new JPanel(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu spmpMenu = new JMenu("Select a gamemode");
        JMenuItem singleplayer = spmpMenu.add(new JMenuItem("Singleplayer"));
        JMenuItem multiplayer = spmpMenu.add(new JMenuItem("Multiplayer"));
        menuBar.add(spmpMenu);
        worldSelector.add(menuBar, BorderLayout.NORTH);
        selectedMode = "SinglePlayer";

        List<String> worlds = journeymap.getWorldList(false);
        DefaultListModel<String> worldListModel = new DefaultListModel<>();
        worldListModel.addAll(worlds);
        JList<String> worldList = new JList<>(worldListModel);

        JScrollPane scrollPane = new JScrollPane(worldList);
        scrollPane.setPreferredSize(new java.awt.Dimension(300, 200));
        worldSelector.add(scrollPane, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton cancelButton = new JButton("Cancel");
        JButton doneButton = new JButton("Done");
        buttons.add(cancelButton);
        buttons.add(doneButton);
        worldSelector.add(buttons, BorderLayout.SOUTH);

        cancelButton.addActionListener(e -> {
            frame.getContentPane().remove(worldSelector);
            frame.revalidate();
            frame.repaint();
            panel.requestFocusInWindow();
        });

        doneButton.addActionListener(e -> {
            journeymap.setWorld(new World(worldList.getSelectedValue(), selectedMode.equals("Multiplayer")));
            frame.getContentPane().remove(worldSelector);
            frame.revalidate();
            frame.repaint();
            panel.requestFocusInWindow();
        });

        ActionListener menuListener = e -> {
            JMenuItem source = (JMenuItem) e.getSource();
            selectedMode = source.getText();
            worldListModel.removeAllElements();
            worldListModel.addAll(journeymap.getWorldList(selectedMode.equals("Multiplayer")));
        };
        singleplayer.addActionListener(menuListener);
        multiplayer.addActionListener(menuListener);

        return worldSelector;
    }
}