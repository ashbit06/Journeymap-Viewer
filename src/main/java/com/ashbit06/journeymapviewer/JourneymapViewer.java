package com.ashbit06.journeymapviewer;

import Journeymap.*;
import Journeymap.Dimension;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class JourneymapViewer extends JPanel {
    private static final int REGION_SIZE = 512;

    private static final Color TOOLBAR_COLOR = new Color(50, 50, 50);
    private static final Color BACKGROUND_COLOR = new Color(25, 25, 25);

    private static Journeymap journeymap;
    private static String minecraftDirectory;
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
        recenter();

        cachedRegions = new HashMap<>();
        cachedWaypoints = new HashMap<>();
        currentDimension = Dimension.OVERWORLD;
        currentMapType = MapType.DAY;

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocus();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int scroll = e.getScrollAmount();

                if (scroll < 0) {
                    zoomAt(0.9f, getMousePosition());
                } else if (scroll > 0) {
                    zoomAt(1.1f, getMousePosition());
                }
            }
        });
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getWidth() <= 0 || getHeight() <= 0) return;

        setViewableRectangle();
        drawRegions(g);
        drawWaypoints(g);
        drawWaypointLabel(g);
        drawOverlays(g);

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

    public void recenter() {
        panX = -(getWidth()/2);
        panY = -(getHeight()/2);
    }

    public Point getCenter() {
        return new Point(
                getWidth() / 2,
                getHeight() / 2
        );
    }

    public Point getMapToScreen(PointXZ p) {
        return new Point(
                (int)((p.x * zoom) - panX),
                (int)((p.z * zoom) - panY)
        );
    }

    public PointXZ getScreenToMap(Point p) {
        return new PointXZ(
                (int)((p.x + panX) / zoom),
                (int)((p.y + panY) / zoom)
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
        if (journeymap.getWorld() == null) return;

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
            }
        }

        // clear cache
        cachedRegions.keySet().removeIf(k -> !usedRegions.contains(k));
    }

    public void drawWaypoints(Graphics g) {
        if (journeymap == null) return;
        if (journeymap.getWorld() == null) return;

        List<String> usedWaypoints = new ArrayList<>();
        List<String> filtered = journeymap.getWaypoints().keySet().stream().filter(
                name -> journeymap.getWaypoints().get(name).primaryDimension() == currentDimension).toList();
        for (String guid : filtered) {
            Waypoint wp = journeymap.getWaypoints().get(guid);

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

            Point drawXY = getMapToScreen(wp.position().getXZ());

            if (!(-wp.icon().getWidth() < drawXY.x && drawXY.x < getWidth()) || !(-wp.icon().getHeight() < drawXY.y && drawXY.y < getHeight()))
                continue;

            g.drawImage(icon, drawXY.x-icon.getWidth()/2, drawXY.y-icon.getHeight()/2, null);

            // clear cache
            usedWaypoints.add(guid);
        }

        cachedWaypoints.keySet().removeIf(k -> !usedWaypoints.contains(k.guid()));
    }

    public void drawWaypointLabel(Graphics g) {
        Point mousePos = getMousePosition();
        if (mousePos == null) return;

        Graphics2D g2 = (Graphics2D) g;
        Font font = g2.getFont();
        FontMetrics metrics = g2.getFontMetrics(font);

        for (Waypoint waypoint : cachedWaypoints.keySet()) {
            Point drawWp = getMapToScreen(waypoint.position().getXZ());
            if (Point.distance(mousePos.x, mousePos.y, drawWp.x, drawWp.y) < 10) {
                String name = waypoint.name();

                int textWidth = metrics.stringWidth(name);
                int textHeight = metrics.getHeight();

                int x = drawWp.x - textWidth / 2 - 2;
                int y = drawWp.y - textHeight / 2 + metrics.getAscent() + waypoint.icon().getHeight() / 2;

                g2.setColor(new Color(0, 0, 0, 128));
                g2.fillRect(x-2, y-2, textWidth+4, textHeight+4);
                g2.setColor(waypoint.icon().getColor());
                g2.drawString(name, x, y+textHeight-4);
            }
        }
    }

    public void drawOverlays(Graphics g) {
        Point mousePos = getMousePosition();
        if (mousePos == null) return;
        PointXZ mapPos = getScreenToMap(mousePos);

        Graphics2D g2 = (Graphics2D) g;
        Font font = g2.getFont();
        FontMetrics metrics = g2.getFontMetrics(font);

        String text = String.format("X: %d   Z: %d", mapPos.x, mapPos.z);

        int textWidth = metrics.stringWidth(text);
        int textHeight = metrics.getHeight();

        int x = (getWidth() - textWidth) / 2;
        int y = getHeight() - 20 - textHeight / 2 + metrics.getAscent();

        g2.setColor(new Color(0, 0, 0, 128));
        g2.fillRect(x-2, y-2, textWidth+4, textHeight+4);
        g2.setColor(Color.white);
        g2.drawString(text, x, y+textHeight-4);
    }

    private static JMenu getDimensionMenu(JourneymapViewer canvas) {
        JMenu dimensionMenu = new JMenu("Dimension");
        ActionListener dimensionMenuListener = e -> {
            JMenuItem source = (JMenuItem) e.getSource();
            String text = source.getText();
            System.out.println(text);
            try {
                canvas.currentDimension = Dimension.from(text.toLowerCase());
                canvas.cachedRegions.keySet().forEach(canvas.cachedRegions::remove);
                canvas.cachedWaypoints.keySet().forEach(canvas.cachedWaypoints::remove);
                if (text.equals(Dimension.NETHER.getId())) {
                    canvas.currentMapType = MapType.CAVE;
                    canvas.caveLayer = 14;
                }
                canvas.repaint();
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
                canvas.currentMapType = MapType.from(text.toLowerCase());
                canvas.cachedRegions.keySet().forEach(canvas.cachedRegions::remove);
                canvas.cachedWaypoints.keySet().forEach(canvas.cachedWaypoints::remove);
                canvas.repaint();
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
            try {
                journeymap.setWorld(new World(worldList.getSelectedValue(), canvas.selectedMode.equals("Multiplayer")));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Settings.saveLastWorld(String.format("%d%s", journeymap.getWorld().isMultiplayer()?1:0, journeymap.getWorld().name()));
            frame.setTitle(String.format("Journeymap Viewer (%s/%s)", journeymap.getPath().getName(), journeymap.getWorld().name()));
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

    private static void restoreSettings() {
        String p = Settings.getLastJourneymap();
        if (p != null) {
            try {
                journeymap = new Journeymap(p);
            } catch (Exception e) {
                System.out.println("Unable to properly restore the last journeymap");
            }
        }

        String w = Settings.getLastWorld();
        if (w != null) {
            try {
                journeymap.setWorld(new World(w.substring(1), w.charAt(0) > 1));
            } catch (IOException e) {
                System.out.println("Unable to properly restore the last world");
            }
        }
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
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT));
        frame.add(toolbar, BorderLayout.NORTH);

        JButton minecraftSelectButton = new JButton("select minecraft");
        toolbar.add(minecraftSelectButton);

        JButton worldSelectButton = new JButton("select world");
        toolbar.add(worldSelectButton);
        worldSelectButton.setEnabled(false);

        JMenu dimensionMenu = getDimensionMenu(canvas);
        toolbar.add(dimensionMenu);
        dimensionMenu.setEnabled(false);

        JMenu mapTypeMenu = getMapTypeMenu(canvas);
        toolbar.add(mapTypeMenu);
        mapTypeMenu.setEnabled(false);

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

                try {
                    journeymap = new Journeymap(minecraftDirectory);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                worldSelectButton.dispatchEvent(new ActionEvent(minecraftSelectButton, ActionEvent.ACTION_FIRST, "idk"));

                if (journeymap != null) {
                    Settings.saveLastJourneymap(journeymap.getPath().getAbsolutePath());
                    if (journeymap.getWorld() != null) canvas.repaint();
                }
            } else {
                System.out.println("File picker canceled");
            }

            if (journeymap != null) {
                worldSelectButton.setEnabled(true);
                mapTypeMenu.setEnabled(true);
            }
        });

        worldSelectButton.addActionListener(e -> {
            frame.getContentPane().add(getWorldSelector(frame, canvas), BorderLayout.CENTER);
            frame.revalidate();
            canvas.requestFocusInWindow();
            mapTypeMenu.setEnabled(true);
            dimensionMenu.setEnabled(true);
        });

        caveLayerSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            int value = source.getValue();
            caveLayerLabel.setText(String.valueOf(value));
            canvas.caveLayer = value;
            canvas.cachedRegions.keySet().forEach(canvas.cachedRegions::remove);
            canvas.repaint();
        });

        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (journeymap == null) return;

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
        });

        frame.setVisible(true);
        canvas.recenter();
        canvas.requestFocus();

        restoreSettings();
        if (journeymap != null) {
            worldSelectButton.setEnabled(true);
            if (journeymap.getWorld() != null) {
                mapTypeMenu.setEnabled(true);
                dimensionMenu.setEnabled(true);
                canvas.repaint();
            }
        }
    }
}
