package wytray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class.
 */
public class Main {
    private static final String DEFAULT_CITY = "Киев";
    private static final long UPDATE_INTERVAL = 5 * 60 * 1000;
    public static final String TOOLTIP = "Обновление каждые 5 мин. Левый щелчек - обновить сейчас";
    public static final String PREFS_CITIES = "cities";
    private static final String PREFS_DEFAULT_CITY = "defaultcity";
    private Map<String, Integer> cities = new HashMap<String, Integer>();
    private final PopupMenu menu;
    private final SystemTray tray;
    private final TrayIcon icon;
    private List<CheckboxMenuItem> cityItems = new ArrayList<CheckboxMenuItem>();
    private CheckboxMenuItem selectedCity;
    private final Preferences prefs;
    private String selectedOnStart = DEFAULT_CITY;
    private Preferences cityPrefs;

    public static void main(String[] args) throws AWTException, BackingStoreException {
        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "Tray not supported", "Error", JOptionPane.ERROR_MESSAGE);
            System.out.println("System tray not supported. Exiting.");
            System.exit(1);
        }
        Main app = new Main();

    }

    public Main() throws AWTException {
        //populate map with default cities
        cities.put("Киев", 924938);
        cities.put("Сарата", 933041);
        cities.put("Берген", 857105);
        cities.put("Константиновка", 923431);
        //try to load user preferences
        prefs = Preferences.userNodeForPackage(Main.class);
        loadCitiesFromPrefs();
        //create system tray
        icon = createIcon();
        menu = createMenu();
        icon.setPopupMenu(menu);
        tray = SystemTray.getSystemTray();
        icon.setImageAutoSize(true);
        tray.add(icon);
//update timer. Update weather each 5min, starting in 30sec from app launch
        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTooltip();
            }
        }, 30000, UPDATE_INTERVAL);

    }

    /**
     * App settings are stored/read from wytray node.
     * defaultcity is the city enabled on start
     * cities subnode contains city=code entries
     */
    private void loadCitiesFromPrefs() {
        cityPrefs = prefs.node(PREFS_CITIES);
        try {
            String[] savedCities = cityPrefs.keys();
            if (savedCities.length == 0) {
                return;
            }
            Map<String, Integer> cityMap = new HashMap<String, Integer>();
            for (String city : savedCities) {
                cityMap.put(city, cityPrefs.getInt(city, 0));
            }
            cities = cityMap;
            selectedOnStart = prefs.get(PREFS_DEFAULT_CITY, DEFAULT_CITY);

        } catch (BackingStoreException e) {
            JOptionPane.showMessageDialog(null, "Tray not supported", "Warning", JOptionPane.WARNING_MESSAGE);
            System.out.println("[WARNING] no support for Preferences API on the system. Settings will not be saved");
        }
    }

    private TrayIcon createIcon() {
        URL imageURL = Main.class.getResource("w.png");
        ImageIcon img = new ImageIcon(imageURL, "wytray icon");
        TrayIcon result = new TrayIcon(img.getImage(), TOOLTIP);
        result.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                updateTooltip();
            }
        });
        result.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    updateTooltip();
                }
            }
        });
        return result;
    }

    private PopupMenu createMenu() {
        PopupMenu result = new PopupMenu();
        Set<String> cts = cities.keySet();
        ItemListener checkboxListener = new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    selectedCity = (CheckboxMenuItem) itemEvent.getSource();
                    updateTooltip();

                    for (CheckboxMenuItem itm : cityItems) {
                        if (itm != selectedCity) {
                            itm.setState(false);
                        }
                    }
                }
            }
        };
        for (String city : cts) {
            CheckboxMenuItem item = new CheckboxMenuItem(city);
            if (city.equals(selectedOnStart)) {
                item.setState(true);
                selectedCity = item;
            }
            cityItems.add(item);
            item.addItemListener(checkboxListener);
            result.add(item);
        }
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                tray.remove(icon);
                savePrefs();
                System.exit(0);
            }
        });
        result.add(exitItem);

        return result;
    }

    private void savePrefs() {
        for (String s : cities.keySet()) {
            cityPrefs.putInt(s, cities.get(s));
        }
        prefs.put(PREFS_DEFAULT_CITY, selectedCity.getLabel());
    }

    private void updateTooltip() {
        String weatherXML = loadWeatherData();
        String tooltip = formatTooltip(weatherXML);
        icon.setToolTip(tooltip);
    }

    private String formatTooltip(String weatherXML) {
        String res = "NA";
        Pattern p = Pattern.compile("<yweather:condition.*temp=\"(.?\\d+)\".*/>");
        Matcher matcher = p.matcher(weatherXML);
        if (matcher.find()) {
            res = matcher.group(1);
        }
        return String.format("%s: %s", selectedCity.getLabel(), res);
    }

    private String loadWeatherData() {
        try {
            URL w = new URL(String.format("http://weather.yahooapis.com/forecastrss?w=%d&u=c", cities.get(selectedCity.getLabel())));
            Scanner scanner = new Scanner(w.openStream(), "UTF-8").useDelimiter("\\A");
            String content = scanner.next();
            scanner.close();
            return content;
        } catch (IOException e) {
            return "";
        }

    }
}
