package tray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main {
    private static final String DEFAULT_CITY = "Киев";
    private static final long UPDATE_INTERVAL = 5 * 60 * 1000;
    public static final String TOOLTIP = "Обновление каждые 5 мин. Левый щелчек - обновить сейчас";
    private Map<String, Integer> cities = new HashMap<String, Integer>();
    private final PopupMenu menu;
    private final SystemTray tray;
    private final TrayIcon icon;
    private List<CheckboxMenuItem> cityItems=new ArrayList<CheckboxMenuItem>();
    private CheckboxMenuItem selectedCity;

    public static void main(String[] args) throws AWTException {
        if(!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null, "Tray not supported", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        Main app = new Main();

    }

    public Main() throws AWTException {
        cities.put("Киев", 924938);
        cities.put("Сарата", 933041);
        cities.put("Берген", 857105);
        cities.put("Константиновка", 923431);
        icon = createIcon();
        menu = createMenu();
        icon.setPopupMenu(menu);
        tray = SystemTray.getSystemTray();
        icon.setImageAutoSize(true);
        tray.add(icon);

        Timer t = new Timer(true);
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateTooltip();
            }
        },30000,UPDATE_INTERVAL);

    }

    private TrayIcon createIcon() {
        URL imageURL = Main.class.getResource("w.png");
        ImageIcon img = new ImageIcon(imageURL, "tray icon");
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

    private  PopupMenu createMenu() {
        PopupMenu result = new PopupMenu();
        Set<String> cts = cities.keySet();
        ItemListener checkboxListener=new ItemListener() {
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
            if (city.equals(DEFAULT_CITY)) {
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
                System.exit(0);
            }
        });
        result.add(exitItem);

        return result;
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

    private String loadWeatherData()  {
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
