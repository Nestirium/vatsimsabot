package nestirium.savacc.utils;

import java.awt.*;

public class Utils {

    public static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

}
