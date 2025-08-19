import Journeymap.*;
import Journeymap.Dimension;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class JourneymapViewer extends JPanel {
    private static final int TOOLBAR_HEIGHT = 40;
    private static final int REGION_SIZE = 512;

    private static final Color TOOLBAR_COLOR = new Color(50, 50, 50);
    private static final Color BACKGROUND_COLOR = new Color(25, 25, 25);

    private static Journeymap journeymap;
    private static String minecraftDirectory;
    private World world;
    private Dimension currentDimension;
    private MapType currentMapType;
    private int caveLayer = 23;
    private String selectedMode;


    private int panX = 0;
    private int panY = 0;
    private final int panSpeed = 16;
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        setViewableRectangle();
        drawRegions(g);
        drawWaypoints(g);

        System.out.println(Arrays.toString(viewableRectangle));
    }

    public void setZoom(float value) {
        if (value <= 0.0f) throw new IllegalArgumentException("Zoom must be > 0");
        zoom = value;
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

    public void drawRegions(Graphics g) {
        if (journeymap == null) return;
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

        HashSet<PointXZ> usedRegions = new HashSet<>();
        for (int regionZ = viewableTopLeft.y; regionZ < viewableBottomRight.y; regionZ++) {
            for (int regionX = viewableTopLeft.x; regionX < viewableBottomRight.x; regionX++) {
                PointXZ regionXZ = new PointXZ(regionX, regionZ);
                BufferedImage region;

                if (cachedRegions.containsKey(regionXZ)) {
                    region = cachedRegions.get(regionXZ);
                } else {
                    try {
                        if (currentMapType == MapType.CAVE) region = journeymap.getCaveRegion(currentDimension, caveLayer, regionXZ);
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
        if (journeymap == null) return;

        HashMap<String, Waypoint> waypoints = journeymap.getWaypoints();
        if (waypoints == null) return;

        List<String> usedWaypoints = new ArrayList<>();
        for (String guid : waypoints.keySet()) {
            Waypoint wp = waypoints.get(guid);

            BufferedImage icon;
            try {
                if (cachedWaypoints.containsKey(wp)) icon = cachedWaypoints.get(wp);
                else {
                    icon = wp.icon().render();
                    cachedWaypoints.put(wp, icon);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            int drawX = -(int)((wp.position().x + viewableRectangle[0]) * zoom);
            int drawY = -(int)((wp.position().y + viewableRectangle[1]) * zoom);

            if (!(-wp.icon().getWidth() < drawX && drawX < getWidth()) || !(-wp.icon().getHeight() < drawY && drawY < getHeight()))
                continue;
            System.out.printf("drawing waypoint \"%s\" at (%d, %d)\n", wp.name(), drawX, drawY);

            g.drawImage(icon, drawX, drawY, null);

            // clear cache
            usedWaypoints.add(guid);
        }

        cachedWaypoints.keySet().removeIf(k -> !usedWaypoints.contains(k.guid()));
    }

    private static JMenu getDimensionMenu(JourneymapViewer canvas) {
        JMenu dimensionMenu = new JMenu("Dimension");
        ActionListener dimensionMenuListener = e -> {
            JMenuItem source = (JMenuItem) e.getSource();
            String text = source.getText();
            try {
                canvas.currentDimension = Dimension.from(text.toLowerCase());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        for (String name : Dimension.names) {
            JMenuItem menuItem = new JMenuItem(name);
            menuItem.addActionListener(dimensionMenuListener);
            dimensionMenu.add(menuItem);
        }
        return dimensionMenu;
    }

    private static JMenu getMapTypeMenu(JourneymapViewer canvas) {
        JMenu mapTypeMenu = new JMenu("Map Type");
        ActionListener mapTypeMenuAction = e -> {
            JMenuItem source = (JMenuItem) e.getSource();
            String text = source.getText();
            try {
                canvas.currentMapType = MapType.from(text.toLowerCase(Locale.ROOT));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
        for (String name : MapType.names) mapTypeMenu.add(new JMenuItem(name)).addActionListener(mapTypeMenuAction);
        return mapTypeMenu;
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

    private static JPanel getWorldSelector(JFrame frame, JourneymapViewer canvas) {
        JPanel worldSelector = new JPanel(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu spmpMenu = new JMenu("Select a gamemode");
        JMenuItem singleplayer = spmpMenu.add(new JMenuItem("Singleplayer"));
        JMenuItem multiplayer = spmpMenu.add(new JMenuItem("Multiplayer"));
        menuBar.add(spmpMenu);
        worldSelector.add(menuBar, BorderLayout.NORTH);
        canvas.selectedMode = "SinglePlayer";

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
            canvas.requestFocusInWindow();
        });

        doneButton.addActionListener(e -> {
            journeymap.setWorld(new World(worldList.getSelectedValue(), canvas.selectedMode.equals("Multiplayer")));
            frame.getContentPane().remove(worldSelector);
            frame.revalidate();
            frame.repaint();
            canvas.requestFocusInWindow();
        });

        ActionListener menuListener = e -> {
            JMenuItem source = (JMenuItem) e.getSource();
            String text = source.getText();
            spmpMenu.setText(text);
            canvas.selectedMode = text;
            worldListModel.removeAllElements();
            worldListModel.addAll(journeymap.getWorldList(canvas.selectedMode.equals("Multiplayer")));
        };
        singleplayer.addActionListener(menuListener);
        multiplayer.addActionListener(menuListener);

        return worldSelector;
    }

    public static void main(String[] args) throws Exception {
        JourneymapViewer canvas = new JourneymapViewer();
        JFrame frame = new JFrame("Journeymap Viewer");
        frame.setIconImage(ImageIO.read(new File("assets/icon.png")));
        frame.setSize(800, 500);
        frame.setMinimumSize(new java.awt.Dimension(800, 500));
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JMenuBar toolbar = new JMenuBar();
        toolbar.setBackground(TOOLBAR_COLOR);
        toolbar.setPreferredSize(new java.awt.Dimension(frame.getWidth(), TOOLBAR_HEIGHT));
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
        frame.add(toolbar, BorderLayout.NORTH);

        JButton minecraftSelectButton = new JButton("select minecraft");
        toolbar.add(minecraftSelectButton);

        JButton worldSelectButton = new JButton("select world");
        toolbar.add(worldSelectButton);

        JMenu dimensionMenu = getDimensionMenu(canvas);
        toolbar.add(dimensionMenu);
        dimensionMenu.setEnabled(false);

        JMenu mapTypeMenu = getMapTypeMenu(canvas);
        toolbar.add(mapTypeMenu);
        mapTypeMenu.setEnabled(false);

        // FIXME: slider never returns focus to the canvas
        JSlider caveLayerSlider = new JSlider(JSlider.HORIZONTAL, -4, 23, canvas.caveLayer);
        JLabel caveLayerLabel = new JLabel(String.valueOf(canvas.caveLayer));
        toolbar.add(caveLayerSlider);
        toolbar.add(caveLayerLabel);

        frame.add(canvas, BorderLayout.CENTER);

        // actions
        minecraftSelectButton.addActionListener(e -> {
            JFileChooser minecraftSelector = getMinecraftSelector();
            int result = minecraftSelector.showOpenDialog(frame);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedDir = minecraftSelector.getSelectedFile();
                minecraftDirectory = selectedDir.getAbsolutePath();
                System.out.println("Selected folder: " + minecraftDirectory);

                if (canvas.world == null) {
                    try {
                        journeymap = new Journeymap(minecraftDirectory);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    worldSelectButton.dispatchEvent(new ActionEvent(minecraftSelectButton, ActionEvent.ACTION_FIRST, "idk"));
                    return;
                }

                canvas.repaint();
            } else {
                System.out.println("File picker canceled");
            }

            if (journeymap != null) {
                mapTypeMenu.setEnabled(true);
            }
        });

        worldSelectButton.addActionListener(e -> {
            frame.getContentPane().add(getWorldSelector(frame, canvas), BorderLayout.CENTER);
            frame.revalidate();
            canvas.requestFocusInWindow();
            canvas.world = journeymap.getWorld();
            mapTypeMenu.setEnabled(true);
            dimensionMenu.setEnabled(true);
        });

        caveLayerSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            int value = source.getValue();
            caveLayerLabel.setText(String.valueOf(value));
            canvas.caveLayer = value;
            canvas.repaint();
        });

        canvas.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();

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
}